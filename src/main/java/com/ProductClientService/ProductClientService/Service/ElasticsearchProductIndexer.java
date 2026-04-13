package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.Model.ProductMetrics;
import com.ProductClientService.ProductClientService.Repository.ProductMetricsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ElasticsearchProductIndexer
 * ────────────────────────────
 * Builds the denormalized ProductSearchDocument from PostgreSQL and
 * pushes it to the "products-v1" Elasticsearch index.
 *
 * Two entry points:
 *
 *  indexProduct(productId)
 *    Called when a product goes LIVE (from SellerService.makeProductLive).
 *    Runs @Async so it never slows down the seller's HTTP response.
 *
 *  bulkReindex(productIds)
 *    Called by ElasticsearchReindexJob or ProductRankingScorerJob when
 *    ranking_score changes need to be pushed to ES in bulk.
 *
 * The native SQL query below is the single-join that replaces the CTE-heavy
 * search query — we pay the join cost ONCE at index time, not at query time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchProductIndexer {

    public static final String INDEX = "products-v1";

    private final ElasticsearchClient esClient;
    private final ProductMetricsRepository metricsRepository;

    @PersistenceContext
    private final EntityManager em;

    // ── Single product (async) ────────────────────────────────────────────────

    @Async
    public void indexProduct(UUID productId) {
        try {
            ProductSearchDocument doc = buildDocument(productId);
            if (doc == null) {
                log.warn("Could not build ES document for productId={}", productId);
                return;
            }
            esClient.index(i -> i.index(INDEX).id(productId.toString()).document(doc));
            log.info("Indexed productId={} to ES", productId);
        } catch (Exception e) {
            log.error("Failed to index productId={}: {}", productId, e.getMessage());
        }
    }

    // ── Bulk update for scorer job ────────────────────────────────────────────

    public void bulkReindex(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        List<BulkOperation> ops = new ArrayList<>();
        for (UUID id : productIds) {
            ProductSearchDocument doc = buildDocument(id);
            if (doc == null) continue;
            ops.add(BulkOperation.of(b -> b.index(
                    IndexOperation.of(i -> i.index(INDEX).id(id.toString()).document(doc)))));
        }

        if (ops.isEmpty()) return;

        try {
            BulkResponse resp = esClient.bulk(BulkRequest.of(b -> b.operations(ops)));
            if (resp.errors()) {
                resp.items().stream()
                        .filter(item -> item.error() != null && item.error().reason() != null)
                        .forEach(item -> log.warn("ES bulk error for id={}: {}",
                                item.id(), item.error().reason()));
            }
            log.info("Bulk-indexed {} products", ops.size());
        } catch (Exception e) {
            log.error("Bulk reindex failed: {}", e.getMessage());
        }
    }

    // ── Document builder ──────────────────────────────────────────────────────

    /**
     * One native SQL query loads all the data needed for the document.
     * Joins: products → brands, categories, sellers, product_variants (cheapest),
     *        product_attributes (images), product_attribute_values (tags + attrs).
     */
    @SuppressWarnings("unchecked")
    private ProductSearchDocument buildDocument(UUID productId) {
        String sql = """
                WITH cheapest AS (
                    SELECT DISTINCT ON (product_id)
                        id AS variant_id, product_id, price, stock,
                        discount_percentage
                    FROM product_variants
                    WHERE stock > 0
                    ORDER BY product_id, CAST(price AS NUMERIC) ASC
                ),
                imgs AS (
                    SELECT pa.product_id,
                           STRING_AGG(img_val, ',' ORDER BY img_val) AS image_urls
                    FROM product_attributes pa
                    JOIN category_attributes ca ON ca.id = pa.category_attribute_id
                        AND ca.is_image_attribute = TRUE
                    CROSS JOIN LATERAL UNNEST(string_to_array(pa.images, ',')) AS img_val
                    GROUP BY pa.product_id
                )
                SELECT
                    p.id, p.name, p.description, p.step,
                    b.id   AS brand_id,   b.name AS brand_name,
                    c.id   AS cat_id,     c.name AS cat_name,
                    s.id   AS seller_id,
                    cv.variant_id,
                    cv.price          AS min_price,
                    cv.price          AS orig_price,
                    cv.discount_percentage,
                    cv.stock,
                    COALESCE(imgs.image_urls,'') AS images,
                    p.average_rating, p.rating_count,
                    p.created_at,
                    2  AS delivery_days,
                    1  AS free_delivery
                FROM products p
                LEFT JOIN brands   b  ON b.id = p.brand_id
                LEFT JOIN categories c ON c.id = p.category_id
                LEFT JOIN sellers  s  ON s.id = p.seller_id
                LEFT JOIN cheapest cv ON cv.product_id = p.id
                LEFT JOIN imgs        ON imgs.product_id = p.id
                WHERE p.id = :productId
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("productId", productId)
                .getResultList();

        if (rows.isEmpty()) return null;
        Object[] r = rows.get(0);

        // ── Attributes ────────────────────────────────────────────────────────
        List<ProductSearchDocument.AttributeEntry> attrs = loadAttributes(productId);

        // ── Metrics ───────────────────────────────────────────────────────────
        ProductMetrics metrics = metricsRepository.findById(productId)
                .orElse(new ProductMetrics(productId));

        // ── Price ─────────────────────────────────────────────────────────────
        long minPricePaise = parseLong(r[10]);
        long origPricePaise = parseLong(r[11]);
        int discPct = 0;
        if (origPricePaise > 0 && minPricePaise < origPricePaise) {
            discPct = (int) Math.round(((origPricePaise - minPricePaise) * 100.0) / origPricePaise);
        }
        String discStr = r[12] != null ? r[12].toString() : null;
        if (discStr != null && !discStr.isBlank()) {
            try { discPct = Integer.parseInt(discStr.split("\\.")[0]); } catch (Exception ignored) {}
        }

        // ── Images list ───────────────────────────────────────────────────────
        String imagesCsv = r[14] != null ? r[14].toString() : "";
        List<String> images = imagesCsv.isBlank()
                ? List.of()
                : Arrays.asList(imagesCsv.split(","));

        return ProductSearchDocument.builder()
                .productId(r[0].toString())
                .name(str(r[1]))
                .description(str(r[2]))
                .step(str(r[3]))
                .brandId(r[4] != null ? r[4].toString() : null)
                .brandName(str(r[5]))
                .categoryId(r[6] != null ? r[6].toString() : null)
                .categoryName(str(r[7]))
                .sellerId(r[8] != null ? r[8].toString() : null)
                .variantId(r[9] != null ? r[9].toString() : null)
                .minPricePaise(minPricePaise)
                .originalPricePaise(origPricePaise)
                .discountPercent(discPct)
                .inStock(parseLong(r[15]) > 0)
                .images(images)
                .avgRating(r[16] != null ? ((Number) r[16]).doubleValue() : 0.0)
                .reviewCount(r[17] != null ? ((Number) r[17]).intValue() : 0)
                .createdAt(r[18] != null
                        ? ((java.sql.Timestamp) r[18]).toInstant()
                        : java.time.Instant.now())
                .deliveryDays(((Number) r[19]).intValue())
                .freeDelivery(((Number) r[20]).intValue() == 1)
                .attributes(attrs)
                // metrics
                .rankingScore(metrics.getRankingScore())
                .numberOfOrders(metrics.getNumberOfOrders())
                .numberOfPurchases(metrics.getNumberOfPurchases())
                .conversionRate(metrics.getConversionRate())
                .clickThroughRate(metrics.getClickThroughRate())
                .wishlistCount(metrics.getWishlistCount())
                .cartAddCount(metrics.getCartAddCount())
                .returnRate(metrics.getReturnRate())
                .recentSales7d(metrics.getRecentSales7d())
                .build();
    }

    /** Loads all non-image product attribute name/value pairs. */
    @SuppressWarnings("unchecked")
    private List<ProductSearchDocument.AttributeEntry> loadAttributes(UUID productId) {
        String sql = """
                SELECT a.name, pa.value
                FROM product_attributes pa
                JOIN category_attributes  ca  ON ca.id  = pa.category_attribute_id
                JOIN category_attribute_mapping cam ON cam.category_attribute_id = ca.id
                JOIN attributes a ON a.id = cam.attribute_id
                WHERE pa.product_id = :pid
                  AND (ca.is_image_attribute IS NULL OR ca.is_image_attribute = false)
                  AND pa.value IS NOT NULL
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("pid", productId)
                .getResultList();

        List<ProductSearchDocument.AttributeEntry> result = new ArrayList<>();
        for (Object[] r : rows) {
            String name  = r[0] != null ? r[0].toString() : null;
            String value = r[1] != null ? r[1].toString() : null;
            if (name != null && value != null) {
                result.add(ProductSearchDocument.AttributeEntry.builder()
                        .name(name).value(value).build());
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String str(Object o)   { return o != null ? o.toString() : null; }
    private long parseLong(Object o) {
        if (o == null) return 0L;
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
}
