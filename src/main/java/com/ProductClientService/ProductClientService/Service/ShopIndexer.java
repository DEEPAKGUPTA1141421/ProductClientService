package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.ProductClientService.ProductClientService.DTO.search.ShopSearchDocument;
import com.ProductClientService.ProductClientService.Model.Address;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ShopIndexer
 * ────────────
 * Builds a denormalized ShopSearchDocument from a Seller entity and
 * upserts it into the "shops-v1" Elasticsearch index.
 *
 * Entry points
 * ────────────
 *  indexSeller(sellerId)   — called async by ShopIndexerConsumer on seller.live event
 *  deindexSeller(sellerId) — called when a seller is suspended (status ≠ ACTIVE)
 *
 * The document is keyed by sellerId (= shopId) so re-indexing is idempotent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopIndexer {

    static final String INDEX = "shops-v1";

    private final ElasticsearchClient esClient;
    private final SellerRepository sellerRepository;
    private final ObjectMapper objectMapper;

    // ── Public API ────────────────────────────────────────────────────────────

    @Async
    public void indexSeller(UUID sellerId) {
        sellerRepository.findById(sellerId).ifPresentOrElse(
                seller -> {
                    try {
                        ShopSearchDocument doc = toDocument(seller);
                        IndexRequest<ShopSearchDocument> req = IndexRequest.of(r -> r
                                .index(INDEX)
                                .id(sellerId.toString())
                                .document(doc));
                        esClient.index(req);
                        log.info("Indexed shop document for sellerId={}", sellerId);
                    } catch (Exception e) {
                        log.error("Failed to index shop for sellerId={}: {}", sellerId, e.getMessage(), e);
                    }
                },
                () -> log.warn("indexSeller: seller not found for id={}", sellerId)
        );
    }

    @Async
    public void deindexSeller(UUID sellerId) {
        try {
            esClient.delete(d -> d.index(INDEX).id(sellerId.toString()));
            log.info("Removed shop document for sellerId={}", sellerId);
        } catch (Exception e) {
            log.warn("deindexSeller failed for sellerId={}: {}", sellerId, e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> buildMediaUrls(Seller seller) {
        List<String> urls = new ArrayList<>();
        if (seller.getProfilePhotoUrl() != null) {
            urls.add(seller.getProfilePhotoUrl());
        }
        String raw = seller.getProfileImageAndVideos();
        if (raw != null && !raw.isBlank()) {
            try {
                List<String> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
                parsed.stream().filter(u -> u != null && !u.isBlank()).forEach(urls::add);
            } catch (Exception e) {
                log.warn("Could not parse profileImageAndVideos for sellerId={}: {}", seller.getId(), e.getMessage());
            }
        }
        return urls;
    }

    // ── Document builder ──────────────────────────────────────────────────────

    private ShopSearchDocument toDocument(Seller seller) {
        Address addr = seller.getAddress();

        // Geo location — null-safe, falls back to [0,0] until seller geocodes address
        ShopSearchDocument.GeoPoint geoPoint = null;
        if (addr != null) {
            BigDecimal lat = addr.getLatitude();
            BigDecimal lon = addr.getLongitude();
            if (lat != null && lon != null) {
                geoPoint = ShopSearchDocument.GeoPoint.builder()
                        .lat(lat.doubleValue())
                        .lon(lon.doubleValue())
                        .build();
            }
        }

        String city          = addr != null ? addr.getCity() : null;
        String categoryId    = seller.getCategory() != null ? seller.getCategory().getId().toString() : null;
        String categoryName  = seller.getCategory() != null ? seller.getCategory().getName() : null;

        return ShopSearchDocument.builder()
                .shopId(seller.getId().toString())
                .displayName(seller.getDisplayName())
                .legalName(seller.getLegalName())
                .tags(List.of())                              // enriched later via admin
                .city(city)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .status(seller.getStatus())
                .isOpen(true)                                 // default open; hours management is Phase 5
                .location(geoPoint)
                .avgRating(0.0)                              // updated via review events
                .reviewCount(0)
                .rankingScore(0.0)
                .logoUrl(buildMediaUrls(seller))
                .createdAt(seller.getCreatedAt() != null
                        ? seller.getCreatedAt().toInstant()
                        : Instant.now())
                .build();
    }
}
