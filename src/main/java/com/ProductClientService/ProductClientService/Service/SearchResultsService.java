package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.search.SearchRequest;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse.SearchProductDto;
import com.ProductClientService.ProductClientService.Repository.SearchResultsRepository;
import com.ProductClientService.ProductClientService.Repository.WishlistRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchResultsService
 * ─────────────────────
 * Orchestrates the search results page:
 *
 * 1. Build a Redis cache key from all request params.
 * 2. On cache hit → deserialise and inject per-user wishlist flags, return.
 * 3. On cache miss → run the native SQL query, map rows → DTOs, cache the
 * user-agnostic portion, inject wishlist flags, return.
 *
 * Cache TTL
 * ─────────
 * • Keyword / category search results: 2 minutes (changes frequently).
 * • Popular / category-only pages: 5 minutes.
 *
 * Security
 * ────────
 * • The cached payload is user-agnostic (no wishlist data).
 * • Wishlist flags are injected AFTER cache retrieval using the userId
 * from the JWT (never cached).
 * • userId is never part of the cache key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchResultsService {

    private final SearchResultsRepository searchRepo;
    private final WishlistRepository wishlistRepo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "search:results:";
    private static final Duration CACHE_TTL_SHORT = Duration.ofMinutes(2);
    private static final Duration CACHE_TTL_LONG = Duration.ofMinutes(5);

    // ─── Public entry point ───────────────────────────────────────────────────

    public SearchResultsResponse search(SearchRequest req, UUID userId) {

        String cacheKey = buildCacheKey(req);

        // ── 1. Try Redis cache ────────────────────────────────────────────────
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                SearchResultsResponse response = objectMapper.readValue(cached, SearchResultsResponse.class);
                // Inject wishlist flags (never cached)
                injectWishlistFlags(response.getProducts(), userId);
                log.debug("Cache HIT for key={}", cacheKey);
                return response;
            } catch (Exception e) {
                log.warn("Cache deserialisation failed for key={}: {}", cacheKey, e.getMessage());
            }
        }

        // ── 2. DB query ───────────────────────────────────────────────────────
        log.debug("Cache MISS for key={}", cacheKey);
        List<Object[]> rows = searchRepo.search(req, userId);
        long total = searchRepo.count(req);

        List<SearchProductDto> products = rows.stream()
                .map(this::mapRow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        SearchResultsResponse response = SearchResultsResponse.builder()
                .totalCount(total)
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .hasMore(((long) req.getPage() + 1) * req.getPageSize() < total)
                .products(products)
                .build();

        // ── 3. Cache user-agnostic response ───────────────────────────────────
        try {
            Duration ttl = (req.getKeyword() != null && !req.getKeyword().isBlank())
                    ? CACHE_TTL_SHORT
                    : CACHE_TTL_LONG;
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), ttl);
        } catch (Exception e) {
            log.warn("Failed to cache response for key={}: {}", cacheKey, e.getMessage());
        }

        // ── 4. Inject per-user wishlist flags ─────────────────────────────────
        injectWishlistFlags(response.getProducts(), userId);

        return response;
    }

    // ─── Row mapper ───────────────────────────────────────────────────────────

    /**
     * Maps a raw Object[] row from the native query to SearchProductDto.
     *
     * Column order must match the SELECT list in SearchResultsRepository.
     */
    private SearchProductDto mapRow(Object[] row) {
        try {
            UUID productId = (UUID) row[0];
            String name = (String) row[1];
            String brandName = (String) row[2];
            UUID brandId = (UUID) row[3];
            String minPriceStr = (String) row[4];
            String origPriceStr = (String) row[5];
            double avgRating = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;
            int ratingCount = row[7] != null ? ((Number) row[7]).intValue() : 0;
            String imageUrls = (String) row[8];
            boolean sponsored = row[9] != null && ((Number) row[9]).intValue() == 1;
            int deliveryDays = row[10] != null ? ((Number) row[10]).intValue() : 3;
            boolean freeDeliv = row[11] != null && ((Number) row[11]).intValue() == 1;
            UUID variantId = (UUID) row[12];

            // ── Price conversion: paise → rupees ─────────────────────────────
            double price = minPriceStr != null
                    ? Double.parseDouble(minPriceStr) / 100.0
                    : 0.0;
            Double originalPrice = origPriceStr != null
                    ? Double.parseDouble(origPriceStr) / 100.0
                    : null;

            // Discount % — only show if meaningfully different
            Integer discountPercent = null;
            if (originalPrice != null && originalPrice > price && originalPrice > 0) {
                discountPercent = (int) Math.round(((originalPrice - price) / originalPrice) * 100);
            }

            // ── Badge logic ───────────────────────────────────────────────────
            // Priority: Sponsored > custom badge > discount
            String badge = null;
            if (!sponsored) {
                if (avgRating >= 4.5 && ratingCount > 1000) {
                    badge = "Bestseller";
                } else if (avgRating >= 4.3) {
                    badge = "Top Rated";
                } else if (discountPercent != null && discountPercent >= 20) {
                    badge = discountPercent + "% Off";
                }
            }

            // ── Delivery text ─────────────────────────────────────────────────
            String deliveryText = buildDeliveryText(deliveryDays, freeDeliv);

            // ── Images ────────────────────────────────────────────────────────
            List<String> images = imageUrls != null && !imageUrls.isBlank()
                    ? Arrays.asList(imageUrls.split(","))
                    : List.of();

            return SearchProductDto.builder()
                    .id(productId)
                    .name(name)
                    .brand(brandName)
                    .brandId(brandId)
                    .price(price)
                    .originalPrice(originalPrice)
                    .discountPercent(discountPercent)
                    .rating(avgRating)
                    .reviewCount(ratingCount)
                    .images(images)
                    .hasVideo(false) // extend later with media-type column
                    .badge(badge)
                    .deliveryText(deliveryText)
                    .freeDelivery(freeDeliv)
                    .isSponsored(sponsored)
                    .isWishlisted(false) // injected separately
                    .variantId(variantId)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to map search row: {}", e.getMessage());
            return null;
        }
    }

    // ─── Wishlist injection ───────────────────────────────────────────────────

    /**
     * Single DB call: load all wishlisted product IDs for the user,
     * then flag each product in O(n) using a HashSet.
     */
    private void injectWishlistFlags(List<SearchProductDto> products, UUID userId) {
        if (userId == null || products == null || products.isEmpty())
            return;

        try {
            // Load wishlist once
            Set<UUID> wishlisted = wishlistRepo.findByUserId(userId)
                    .map(wl -> wl.getItems().stream()
                            .map(i -> i.getProductId())
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());

            products.forEach(p -> p.setWishlisted(wishlisted.contains(p.getId())));
        } catch (Exception e) {
            log.warn("Wishlist injection failed for userId={}: {}", userId, e.getMessage());
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private String buildDeliveryText(int days, boolean free) {
        String suffix = free ? ", Free" : "";
        return switch (days) {
            case 0 -> "Today" + suffix;
            case 1 -> "Tomorrow" + suffix;
            default -> "In " + days + " Days" + (free ? ", Free" : "");
        };
    }

    /**
     * Builds a deterministic cache key from all request params.
     * userId is intentionally excluded — wishlist flags are injected post-cache.
     */
    private String buildCacheKey(SearchRequest req) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX);
        sb.append("kw=").append(req.getKeyword() == null ? "" : req.getKeyword().toLowerCase().trim());
        sb.append(":cat=").append(req.getCategoryId());
        sb.append(":sort=").append(req.getSortBy());
        sb.append(":p=").append(req.getPage());
        sb.append(":ps=").append(req.getPageSize());
        sb.append(":minP=").append(req.getMinPrice());
        sb.append(":maxP=").append(req.getMaxPrice());
        sb.append(":rat=").append(req.getMinRating());
        sb.append(":disc=").append(req.getMinDiscountPercent());
        sb.append(":fd=").append(req.getFreeDelivery());
        sb.append(":sd=").append(req.getSameDay());
        sb.append(":tm=").append(req.getTomorrow());
        sb.append(":bs=").append(req.getBestsellers());
        sb.append(":tr=").append(req.getTopRated());
        sb.append(":na=").append(req.getNewArrivals());
        sb.append(":attr=").append(req.getAttributeName());
        if (req.getAttributeValues() != null) {
            sb.append(":attrV=").append(
                    req.getAttributeValues().stream().sorted().collect(Collectors.joining(",")));
        }
        if (req.getBrandIds() != null) {
            sb.append(":brands=").append(
                    req.getBrandIds().stream().map(UUID::toString).sorted().collect(Collectors.joining(",")));
        }
        return sb.toString();
    }

    /**
     * Evict all cached search results pages for a category when new products go
     * live. Called from SellerService.MakeProductLive().
     */
    public void evictCategoryCache(UUID categoryId) {
        try {
            Set<String> keys = redis.keys(CACHE_PREFIX + "*cat=" + categoryId + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("Evicted {} cache entries for categoryId={}", keys.size(), categoryId);
            }
        } catch (Exception e) {
            log.warn("Cache eviction failed for categoryId={}: {}", categoryId, e.getMessage());
        }
    }
}