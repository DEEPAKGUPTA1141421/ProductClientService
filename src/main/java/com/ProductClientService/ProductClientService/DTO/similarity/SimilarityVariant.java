package com.ProductClientService.ProductClientService.DTO.similarity;

/**
 * Similarity variants exposed on the product detail page.
 *
 * ALSO_VIEWED     — neighbours in embedding space, same category allowed
 *                   to differ (Flipkart "Customers also viewed").
 * COMPLETE_LOOK   — neighbours from complementary categories (e.g. shoes
 *                   for a dress). Requires category_pairings config — for
 *                   now same as ALSO_VIEWED but boosts cross-category hits.
 * SIMILAR_CHEAPER — same category, price ≤ 85% of source product. Critical
 *                   for price-sensitive Tier-2/3 shoppers.
 */
public enum SimilarityVariant {
    ALSO_VIEWED,
    COMPLETE_LOOK,
    SIMILAR_CHEAPER;

    public static SimilarityVariant parse(String s) {
        if (s == null || s.isBlank()) return ALSO_VIEWED;
        try { return SimilarityVariant.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return ALSO_VIEWED; }
    }
}
