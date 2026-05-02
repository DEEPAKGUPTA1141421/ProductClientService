package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.network.DeliveryEstimateDto;
import com.ProductClientService.ProductClientService.DTO.search.*;
import com.ProductClientService.ProductClientService.network.DeliveryInventoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ShopService
 * ────────────
 * Orchestrates the shop listing flow:
 *
 * 1. Query Elasticsearch (nearby or text search) via ShopSearchService.
 * 2. Concurrently enrich each shop with a delivery ETA from
 * DeliveryInventoryService
 * (one Feign call per shop, run in parallel on a virtual-thread executor).
 * 3. Apply post-query filters (maxDeliveryMinutes).
 * 4. Package result into ShopPageResponse.
 *
 * Delivery enrichment
 * ────────────────────
 * Feign calls are dispatched concurrently using CompletableFuture so that
 * enriching a page of 20 shops takes ~1 network RTT instead of 20×.
 * On any individual failure the shop is still returned with etaLabel="—"
 * and distanceKm=0 so the listing is never empty due to a delivery-service
 * blip.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopSearchService shopSearchService;
    private final DeliveryInventoryClient deliveryClient;

    private static final ExecutorService DELIVERY_EXECUTOR =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "delivery-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Primary listing: ACTIVE shops near the user, sorted by distance (default).
     */
    public ShopPageResponse getNearbyShops(ShopFilterRequest req) {
        List<ShopSearchDocument> docs = shopSearchService.nearby(req);
        return buildPageResponse(docs, req);
    }

    /**
     * Text search across shop names, tags, categories.
     */
    public ShopPageResponse searchShops(ShopFilterRequest req) {
        List<ShopSearchDocument> docs = shopSearchService.search(req);
        return buildPageResponse(docs, req);
    }

    /**
     * Autocomplete prefix suggestions for the search bar.
     */
    public List<String> getSuggestions(String prefix, int limit) {
        return shopSearchService.suggest(prefix, limit);
    }

    /**
     * Full shop detail, enriched with delivery ETA.
     */
    public ShopDetailDto getShopDetail(String shopId, double userLat, double userLng) {
        ShopSearchDocument doc = shopSearchService.getById(shopId);
        if (doc == null)
            return null;

        DeliveryEstimateDto eta = fetchEta(doc, userLat, userLng);
        return toDetailDto(doc, eta);
    }

    // ── Page assembly ─────────────────────────────────────────────────────────

    private ShopPageResponse buildPageResponse(List<ShopSearchDocument> docs, ShopFilterRequest req) {
        if (docs.isEmpty()) {
            return ShopPageResponse.builder()
                    .content(List.of())
                    .page(req.getPage())
                    .pageSize(req.getPageSize())
                    .totalElements(0)
                    .last(true)
                    .build();
        }

        // Fire all ETA calls concurrently
        List<CompletableFuture<ShopSummaryDto>> futures = docs.stream()
                .map(doc -> CompletableFuture.supplyAsync(
                        () -> toSummaryDto(doc, fetchEta(doc, req.getUserLat(), req.getUserLng())),
                        DELIVERY_EXECUTOR))
                .toList();

        List<ShopSummaryDto> enriched = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Post-query filter: maxDeliveryMinutes
        List<ShopSummaryDto> filtered = applyPostFilters(enriched, req);

        // hasMore: if ES returned a full page there are likely more results
        boolean hasMore = docs.size() == req.getPageSize();

        return ShopPageResponse.builder()
                .content(filtered)
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .totalElements(filtered.size() + (long) req.getPage() * req.getPageSize())
                .last(!hasMore)
                .build();
    }

    // ── Post-query filters ────────────────────────────────────────────────────

    private List<ShopSummaryDto> applyPostFilters(List<ShopSummaryDto> shops, ShopFilterRequest req) {
        if (req.getMaxDeliveryMinutes() == null)
            return shops;

        // etaMinutes is not on ShopSummaryDto — filter is approximated:
        // distanceKm proxy (1 km ≈ 4 min last-mile at 15 kph)
        double maxKm = req.getMaxDeliveryMinutes() / 4.0;
        return shops.stream()
                .filter(s -> s.getDistanceKm() <= maxKm || s.getDistanceKm() == 0.0)
                .toList();
    }

    // ── Delivery ETA fetch ────────────────────────────────────────────────────

    private DeliveryEstimateDto fetchEta(ShopSearchDocument doc, double userLat, double userLng) {
        if (doc.getLocation() == null || (userLat == 0.0 && userLng == 0.0)) {
            return null;
        }
        try {
            return deliveryClient.getDeliveryEstimate(
                    doc.getLocation().getLat(),
                    doc.getLocation().getLon(),
                    userLat,
                    userLng);
        } catch (Exception e) {
            log.warn("Delivery estimate failed for shop={}: {}", doc.getShopId(), e.getMessage());
            return null;
        }
    }

    // ── Document → DTO mappings ───────────────────────────────────────────────

    private ShopSummaryDto toSummaryDto(ShopSearchDocument doc, DeliveryEstimateDto eta) {
        String etaLabel = eta != null ? eta.getEtaLabel() : "—";
        double distanceKm = eta != null ? eta.getTotalKm() : 0.0;

        return ShopSummaryDto.builder()
                .shopId(parseUuid(doc.getShopId()))
                .displayName(doc.getDisplayName())
                .logoUrl(doc.getLogoUrl())
                .city(doc.getCity())
                .categoryName(doc.getCategoryName())
                .categoryId(parseUuid(doc.getCategoryId()))
                .avgRating(doc.getAvgRating())
                .reviewCount(doc.getReviewCount())
                .isOpen(doc.isOpen())
                .deliveryEtaLabel(etaLabel)
                .distanceKm(distanceKm)
                .build();
    }

    private ShopDetailDto toDetailDto(ShopSearchDocument doc, DeliveryEstimateDto eta) {
        String etaLabel = eta != null ? eta.getEtaLabel() : "—";
        double distanceKm = eta != null ? eta.getTotalKm() : 0.0;
        int etaMinutes = eta != null ? eta.getEtaMinutes() : 0;
        boolean sameCity = eta != null && eta.isSameCityDelivery();

        double lat = doc.getLocation() != null ? doc.getLocation().getLat() : 0.0;
        double lon = doc.getLocation() != null ? doc.getLocation().getLon() : 0.0;

        return ShopDetailDto.builder()
                .shopId(parseUuid(doc.getShopId()))
                .displayName(doc.getDisplayName())
                .legalName(doc.getLegalName())
                .logoUrl(doc.getLogoUrl())
                .city(doc.getCity())
                .shopLat(lat)
                .shopLng(lon)
                .categoryName(doc.getCategoryName())
                .categoryId(parseUuid(doc.getCategoryId()))
                .avgRating(doc.getAvgRating())
                .reviewCount(doc.getReviewCount())
                .isOpen(doc.isOpen())
                .tags(doc.getTags())
                .deliveryEtaLabel(etaLabel)
                .distanceKm(distanceKm)
                .etaMinutes(etaMinutes)
                .sameCityDelivery(sameCity)
                .build();
    }

    private UUID parseUuid(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }
}
