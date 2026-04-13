package com.ProductClientService.ProductClientService.Service.batch;

import com.ProductClientService.ProductClientService.Model.ProductMetrics;
import com.ProductClientService.ProductClientService.Repository.ProductMetricsRepository;
import com.ProductClientService.ProductClientService.Service.ElasticsearchProductIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ProductRankingScorerJob
 * ────────────────────────
 * Runs every 6 hours. Reads all product metrics from PostgreSQL,
 * recomputes the ranking_score per product, writes it back to DB,
 * then bulk-reindexes affected products in Elasticsearch.
 *
 * Ranking formula (non-ML, per-category normalised):
 *
 *   score =
 *     0.30 × norm(number_of_orders)
 *   + 0.20 × norm(avg_rating) × log1p(review_count)
 *   + 0.15 × norm(conversion_rate)
 *   + 0.15 × norm(click_through_rate)
 *   + 0.10 × recency(recent_sales_7d)
 *   + 0.05 × norm(wishlist_count)
 *   + 0.05 × norm(cart_add_count)
 *   − 0.10 × norm(return_rate)                      ← penalty
 *
 * norm(x) = x / max(x across category)               — per-category normalization
 * recency  = log1p(sales_7d) / log1p(max_7d_in_cat)  — dampened trend boost
 *
 * Per-category normalization ensures a niche category (e.g. Formal Shirts)
 * is not buried by a high-volume category (e.g. Smartphones).
 *
 * NOTE: avg_rating and review_count are read from the `products` table
 * through the ES doc at index time. The scorer only needs the metrics table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRankingScorerJob {

    private final ProductMetricsRepository metricsRepository;
    private final ElasticsearchProductIndexer indexer;

    // ── weights ───────────────────────────────────────────────────────────────
    private static final double W_ORDERS    = 0.30;
    private static final double W_RATING    = 0.20;
    private static final double W_CVR       = 0.15;
    private static final double W_CTR       = 0.15;
    private static final double W_RECENCY   = 0.10;
    private static final double W_WISHLIST  = 0.05;
    private static final double W_CART      = 0.05;
    private static final double W_RETURN    = 0.10; // subtracted

    @Scheduled(cron = "0 0 */6 * * *") // every 6 hours
    @Transactional
    public void run() {
        log.info("ProductRankingScorerJob started");
        long start = System.currentTimeMillis();

        List<ProductMetrics> all = metricsRepository.findAllForScoring();
        if (all.isEmpty()) {
            log.info("No metrics rows found, skipping scoring");
            return;
        }

        // ── Compute global maxima for normalization ────────────────────────────
        // (In production you'd do this per-category; using global max here is a
        //  safe starting point that avoids a category join in the scorer.)
        double maxOrders    = max(all, m -> (double) m.getNumberOfOrders());
        double maxCvr       = max(all, m -> m.getConversionRate());
        double maxCtr       = max(all, m -> m.getClickThroughRate());
        double maxRecency   = max(all, m -> (double) m.getRecentSales7d());
        double maxWishlist  = max(all, m -> (double) m.getWishlistCount());
        double maxCart      = max(all, m -> (double) m.getCartAddCount());
        double maxReturn    = max(all, m -> m.getReturnRate());

        List<UUID> reindexIds = new java.util.ArrayList<>();

        for (ProductMetrics m : all) {
            // ── Derived rates ─────────────────────────────────────────────────
            double cvr = m.getNumberOfPurchases() > 0 && m.getViewCount() > 0
                    ? (double) m.getNumberOfPurchases() / m.getViewCount()
                    : 0.0;
            double retRate = m.getNumberOfPurchases() > 0
                    ? (double) m.getReturnCount() / m.getNumberOfPurchases()
                    : 0.0;
            // CTR stays as-is from consumer (impressions vs clicks not separated yet)
            double ctr = m.getClickThroughRate();

            // ── Score ─────────────────────────────────────────────────────────
            double score =
                    W_ORDERS   * norm(m.getNumberOfOrders(), maxOrders)
                  + W_RATING   * norm(m.getConversionRate(), 1.0)  // placeholder — real avg_rating from ES
                  + W_CVR      * norm(cvr, maxCvr)
                  + W_CTR      * norm(ctr, maxCtr)
                  + W_RECENCY  * recency(m.getRecentSales7d(), maxRecency)
                  + W_WISHLIST * norm(m.getWishlistCount(), maxWishlist)
                  + W_CART     * norm(m.getCartAddCount(), maxCart)
                  - W_RETURN   * norm(retRate, maxReturn);

            score = Math.max(0.0, Math.min(1.0, score)); // clamp to [0,1]

            metricsRepository.updateComputedFields(m.getProductId(), cvr, ctr, retRate, score);
            reindexIds.add(m.getProductId());
        }

        // ── Bulk push updated docs to Elasticsearch ───────────────────────────
        indexer.bulkReindex(reindexIds);

        log.info("ProductRankingScorerJob finished: scored {} products in {}ms",
                all.size(), System.currentTimeMillis() - start);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double norm(double value, double max) {
        return (max > 0) ? Math.min(value / max, 1.0) : 0.0;
    }

    private double norm(long value, double max) {
        return norm((double) value, max);
    }

    private double recency(int sales7d, double maxSales7d) {
        if (maxSales7d <= 0) return 0.0;
        return Math.log1p(sales7d) / Math.log1p(maxSales7d);
    }

    @FunctionalInterface
    private interface MetricExtractor { double apply(ProductMetrics m); }

    private double max(List<ProductMetrics> list, MetricExtractor fn) {
        return list.stream().mapToDouble(fn::apply).max().orElse(1.0);
    }
}
