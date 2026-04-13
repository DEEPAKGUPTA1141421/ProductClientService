package com.ProductClientService.ProductClientService.DTO.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ProductSearchDocument
 * ──────────────────────
 * Fully denormalized Elasticsearch document for index "products-v1".
 *
 * One document = one LIVE product. No joins at query time.
 *
 * Indexed by: ElasticsearchProductIndexer (on product going LIVE, on metrics update)
 * Queried by: ElasticsearchSearchService
 *
 * ES mapping highlights (set once via PUT /products-v1/_mapping):
 *   name, description         → text (standard analyzer)
 *   brand_name, category_id   → keyword
 *   attributes                → nested  (filter by name+value pair)
 *   min_price_paise            → long
 *   avg_rating, ranking_score  → float
 *   in_stock, step             → keyword
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchDocument {

    // ── Identity ──────────────────────────────────────────────────────────────
    @JsonProperty("product_id")
    private String productId;           // UUID as string (ES _id)

    // ── Searchable text fields ────────────────────────────────────────────────
    private String name;
    private String description;
    private List<String> tags;

    // ── Filterable keyword fields ─────────────────────────────────────────────
    @JsonProperty("brand_id")
    private String brandId;

    @JsonProperty("brand_name")
    private String brandName;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("seller_id")
    private String sellerId;

    /** "LIVE", "PRODUCT_VARIANT", etc. — only LIVE docs should appear in search. */
    private String step;

    @JsonProperty("in_stock")
    private boolean inStock;

    // ── Price & discount ──────────────────────────────────────────────────────
    /** Stored in paise (integer) for fast range filtering without CAST. */
    @JsonProperty("min_price_paise")
    private long minPricePaise;

    @JsonProperty("original_price_paise")
    private long originalPricePaise;

    @JsonProperty("discount_percent")
    private int discountPercent;

    // ── Cheapest variant (for cart/checkout direct linking) ───────────────────
    @JsonProperty("variant_id")
    private String variantId;

    // ── Delivery ──────────────────────────────────────────────────────────────
    @JsonProperty("delivery_days")
    private int deliveryDays;

    @JsonProperty("free_delivery")
    private boolean freeDelivery;

    // ── Media ─────────────────────────────────────────────────────────────────
    private List<String> images;

    // ── Rating ────────────────────────────────────────────────────────────────
    @JsonProperty("avg_rating")
    private double avgRating;

    @JsonProperty("review_count")
    private int reviewCount;

    // ── Nested: attribute filters ─────────────────────────────────────────────
    /**
     * Each element represents one product attribute.
     * Mapped as ES "nested" so a query for {name=Storage, value=128GB} cannot
     * accidentally match against two different attributes of the same product.
     */
    private List<AttributeEntry> attributes;

    // ── Business metrics (updated by scorer batch job) ────────────────────────
    @JsonProperty("ranking_score")
    private double rankingScore;

    @JsonProperty("number_of_orders")
    private long numberOfOrders;

    @JsonProperty("number_of_purchases")
    private long numberOfPurchases;

    @JsonProperty("conversion_rate")
    private double conversionRate;

    @JsonProperty("click_through_rate")
    private double clickThroughRate;

    @JsonProperty("wishlist_count")
    private long wishlistCount;

    @JsonProperty("cart_add_count")
    private long cartAddCount;

    @JsonProperty("return_rate")
    private double returnRate;

    @JsonProperty("recent_sales_7d")
    private int recentSales7d;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @JsonProperty("created_at")
    private Instant createdAt;

    // ── Nested attribute entry ────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeEntry {
        private String name;
        private String value;
    }
}
