package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Product-level business metrics.
 *
 * One row per product. Written exclusively via MetricsWriterService using
 * atomic SQL increments — no read-modify-write to avoid race conditions.
 *
 * Columns owned by Kafka consumers (near real-time):
 * view_count, cart_add_count, wishlist_count, number_of_purchases,
 * number_of_orders, return_count, recent_sales_7d, recent_sales_30d
 *
 * Columns owned by ProductRankingScorerJob (every 6 h):
 * conversion_rate, click_through_rate, return_rate, ranking_score,
 * last_scored_at
 */
@Entity
@Table(name = "product_metrics")
@Getter
@Setter
@NoArgsConstructor
public class ProductMetrics {

    /** Same UUID as the Product — makes the JOIN trivial and the PK lookup O(1). */
    @Id
    @Column(name = "product_id")
    private UUID productId;

    // ── Volume signals (incremented by consumers) ─────────────────────────────

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "search_impression_count", nullable = false)
    private Long searchImpressionCount = 0L;

    @Column(name = "cart_add_count", nullable = false)
    private Long cartAddCount = 0L;

    @Column(name = "wishlist_count", nullable = false)
    private Long wishlistCount = 0L;

    // ── Conversion signals (incremented by order consumers) ───────────────────

    @Column(name = "number_of_purchases", nullable = false)
    private Long numberOfPurchases = 0L;

    @Column(name = "number_of_orders", nullable = false)
    private Long numberOfOrders = 0L;

    @Column(name = "return_count", nullable = false)
    private Long returnCount = 0L;

    // ── Trend signals ─────────────────────────────────────────────────────────

    /** Rolling 7-day purchase count. Reset to 0 daily by RecentSalesResetJob. */
    @Column(name = "recent_sales_7d", nullable = false)
    private Integer recentSales7d = 0;

    /** Accumulated 30-day purchase count. Rolled from 7d by RecentSalesResetJob. */
    @Column(name = "recent_sales_30d", nullable = false)
    private Integer recentSales30d = 0;

    // ── Derived rates (recomputed by ProductRankingScorerJob every 6 h) ───────

    /** purchases / views — computed, not incremented. */
    @Column(name = "conversion_rate", nullable = false)
    private Double conversionRate = 0.0;

    /** clicks / impressions — computed, not incremented. */
    @Column(name = "click_through_rate", nullable = false)
    private Double clickThroughRate = 0.0;

    /** returns / purchases — computed, not incremented. */
    @Column(name = "return_rate", nullable = false)
    private Double returnRate = 0.0;

    // ── Precomputed ranking score (written by scorer, read by ES indexer) ─────

    @Column(name = "ranking_score", nullable = false)
    private Double rankingScore = 0.0;

    @Column(name = "last_scored_at")
    private Instant lastScoredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ProductMetrics(UUID productId) {
        this.productId = productId;
    }
}
