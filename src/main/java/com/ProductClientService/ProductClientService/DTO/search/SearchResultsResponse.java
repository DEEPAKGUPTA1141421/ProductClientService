package com.ProductClientService.ProductClientService.DTO.search;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Unified response for the Search Results Page.
 * All data the Flutter page needs in ONE API call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultsResponse {

    private long totalCount;
    private int page;
    private int pageSize;
    private boolean hasMore;

    private List<SearchProductDto> products;

    // ── Nested DTOs ──────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchProductDto {
        private UUID id;
        private String name;
        private String brand;
        private UUID brandId;

        // Pricing (stored as paise in DB → converted to ₹ here)
        private double price;
        private Double originalPrice; // null if no discount
        private Integer discountPercent; // null if no discount

        private double rating;
        private int reviewCount;

        // Images: first image from image-attribute
        private List<String> images;
        private boolean hasVideo;

        // Badge: "Bestseller" | "30% Off" | "Top Rated" | "New Launch" | null
        private String badge;

        // Delivery
        private String deliveryText;
        private boolean freeDelivery;

        private boolean isSponsored;
        private boolean isWishlisted; // requires userId from JWT

        private UUID variantId; // cheapest in-stock variant
    }
}