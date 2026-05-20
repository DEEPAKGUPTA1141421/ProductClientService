package com.ProductClientService.ProductClientService.DTO.search;

import lombok.*;
import java.util.List;

/**
 * Storefront response — structured sections for a seller's shop page.
 *
 * Section order (backend-decided):
 *   1. BUY_AGAIN  — products the authenticated user has previously ordered from this seller
 *   2. CATEGORY   — one section per unique category, sorted by product count descending
 *
 * Each section carries the first {@code PRODUCTS_PER_RAIL} products plus a
 * {@code totalCount} so the frontend can show "See All (N)" when needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorefrontResponse {

    private List<StorefrontSection> sections;

    // ── Section ──────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorefrontSection {

        public enum SectionType { BUY_AGAIN, CATEGORY }

        private SectionType type;

        /** Display name, e.g. "Buy Again" or "Women's Tops" */
        private String title;

        /** Secondary label, e.g. "3 items you've ordered" or "12 products" */
        private String subtitle;

        /** null for BUY_AGAIN sections; category UUID string for CATEGORY sections */
        private String categoryId;

        /** 0 = highest priority (BUY_AGAIN), then ascending per category */
        private int priority;

        /** Total products in this group — may be > products.size() */
        private long totalCount;

        /** First N products for the horizontal rail; frontend fetches more via search */
        private List<SearchResultsResponse.SearchProductDto> products;
    }
}
