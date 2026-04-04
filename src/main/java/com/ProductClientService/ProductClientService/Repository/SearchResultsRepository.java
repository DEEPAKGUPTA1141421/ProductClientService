package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.DTO.search.SearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * SearchResultsRepository
 * ────────────────────────
 * Executes a single optimised native query that:
 * - Joins products → variants → product_attributes → sellers → brands
 * - Applies all filter/sort params from SearchRequest dynamically
 * - Returns only the columns the Flutter page actually needs
 * - Uses LIMIT / OFFSET for pagination
 *
 * Performance notes
 * ─────────────────
 * 1. The query touches the cheapest-in-stock variant per product via a
 * correlated subquery (or CTE); no N+1 fetching.
 * 2. All filter predicates are guarded by a null-check so the query plan
 * degrades gracefully when filters are absent.
 * 3. A partial index on products(step) WHERE step = 'LIVE' is assumed on
 * the PostgreSQL side (see migration notes at the bottom).
 * 4. Redis caching is applied at the service layer (not here) so the repo
 * stays pure-SQL.
 */
@Repository
public class SearchResultsRepository {

    @PersistenceContext
    private EntityManager em;

    // ─── Main search ──────────────────────────────────────────────────────────

    /**
     * Returns raw Object[] rows. Each row maps to SearchProductProjection fields
     * in the same column order as the SELECT list below.
     *
     * Columns (0-based index):
     * 0 product_id UUID
     * 1 product_name TEXT
     * 2 brand_name TEXT (nullable)
     * 3 brand_id UUID (nullable)
     * 4 min_price TEXT (paise, cheapest in-stock variant)
     * 5 original_price TEXT (paise, MRP – same variant)
     * 6 avg_rating FLOAT8
     * 7 rating_count INT
     * 8 image_urls TEXT (comma-separated)
     * 9 is_sponsored INT (0/1)
     * 10 delivery_days INT
     * 11 free_delivery INT (0/1)
     * 12 variant_id UUID
     */
    public List<Object[]> search(SearchRequest req, UUID userId) {

        StringBuilder sql = new StringBuilder("""
                WITH cheapest_variant AS (
                    SELECT DISTINCT ON (pv.product_id)
                        pv.id          AS variant_id,
                        pv.product_id,
                        pv.price       AS min_price,
                        pv.price       AS original_price,
                        pv.stock
                    FROM product_variants pv
                    WHERE pv.stock > 0
                    ORDER BY pv.product_id, CAST(pv.price AS NUMERIC) ASC
                ),
                product_images AS (
                    SELECT
                        pa.product_id,
                        STRING_AGG(img_val, ',' ORDER BY img_val) AS image_urls
                    FROM product_attributes pa
                    JOIN category_attributes ca ON ca.id = pa.category_attribute_id
                        AND ca.is_image_attribute = TRUE
                    CROSS JOIN LATERAL UNNEST(string_to_array(pa.images, ',')) AS img_val
                    GROUP BY pa.product_id
                )
                SELECT
                    p.id                                        AS product_id,
                    p.name                                      AS product_name,
                    b.name                                      AS brand_name,
                    b.id                                        AS brand_id,
                    cv.min_price,
                    cv.original_price,
                    COALESCE(p.average_rating, 0)               AS avg_rating,
                    COALESCE(p.rating_count, 0)                 AS rating_count,
                    COALESCE(pi_agg.image_urls, '')             AS image_urls,
                    0                                           AS is_sponsored,
                    2                                           AS delivery_days,
                    1                                           AS free_delivery,
                    cv.variant_id
                FROM products p
                JOIN cheapest_variant cv   ON cv.product_id = p.id
                LEFT JOIN brands b         ON b.id = p.brand_id
                LEFT JOIN product_images pi_agg ON pi_agg.product_id = p.id
                WHERE p.step = 'LIVE'
                """);

        Map<String, Object> params = new LinkedHashMap<>();

        // ── Keyword filter ────────────────────────────────────────────────────
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            sql.append("""
                    AND (
                        p.name        ILIKE CONCAT('%', :keyword, '%')
                        OR p.description ILIKE CONCAT('%', :keyword, '%')
                        OR b.name     ILIKE CONCAT('%', :keyword, '%')
                    )
                    """);
            params.put("keyword", req.getKeyword().trim());
        }

        // ── Category filter ───────────────────────────────────────────────────
        if (req.getCategoryId() != null) {
            sql.append("AND p.category_id = :categoryId\n");
            params.put("categoryId", req.getCategoryId());
        }

        // ── Brand filter ──────────────────────────────────────────────────────
        if (req.getBrandIds() != null && !req.getBrandIds().isEmpty()) {
            sql.append("AND p.brand_id = ANY(:brandIds)\n");
            params.put("brandIds", req.getBrandIds().toArray(new UUID[0]));
        }

        // ── Price filter ──────────────────────────────────────────────────────
        if (req.getMinPrice() != null) {
            // Convert ₹ → paise
            sql.append("AND CAST(cv.min_price AS NUMERIC) >= :minPrice\n");
            params.put("minPrice", req.getMinPrice() * 100);
        }
        if (req.getMaxPrice() != null) {
            sql.append("AND CAST(cv.min_price AS NUMERIC) <= :maxPrice\n");
            params.put("maxPrice", req.getMaxPrice() * 100);
        }

        // ── Rating filter ─────────────────────────────────────────────────────
        if (req.getMinRating() != null) {
            sql.append("AND p.average_rating >= :minRating\n");
            params.put("minRating", req.getMinRating());
        }

        // ── Discount filter ───────────────────────────────────────────────────
        if (req.getMinDiscountPercent() != null && req.getMinDiscountPercent() > 0) {
            // discount% = ((original - price) / original) * 100
            // We simplify: assume products with discount have discount_percentage stored
            sql.append("""
                    AND EXISTS (
                        SELECT 1 FROM product_variants pv2
                        WHERE pv2.id = cv.variant_id
                          AND pv2.discount_percentage IS NOT NULL
                          AND CAST(pv2.discount_percentage AS NUMERIC) >= :minDiscount
                    )
                    """);
            params.put("minDiscount", req.getMinDiscountPercent());
        }

        // ── Attribute filter (e.g. storage=256 GB) ────────────────────────────
        if (req.getAttributeName() != null && req.getAttributeValues() != null
                && !req.getAttributeValues().isEmpty()) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM product_attributes  pa2
                        JOIN category_attributes ca2 ON ca2.id = pa2.category_attribute_id
                        JOIN attributes a2          ON a2.id  = ANY(
                                SELECT attribute_id FROM category_attribute_mapping
                                WHERE category_attribute_id = ca2.id
                            )
                        WHERE pa2.product_id = p.id
                          AND LOWER(a2.name) = LOWER(:attrName)
                          AND LOWER(pa2.value) = ANY(:attrValues)
                    )
                    """);
            params.put("attrName", req.getAttributeName());
            String[] lowerValues = req.getAttributeValues().stream()
                    .map(String::toLowerCase)
                    .toArray(String[]::new);
            params.put("attrValues", lowerValues);
        }

        // ── Top-rated filter ──────────────────────────────────────────────────
        if (Boolean.TRUE.equals(req.getTopRated())) {
            sql.append("AND p.average_rating >= 4.0\n");
        }

        // ── New arrivals filter ───────────────────────────────────────────────
        if (Boolean.TRUE.equals(req.getNewArrivals())) {
            sql.append("AND p.created_at >= NOW() - INTERVAL '30 days'\n");
        }

        // ── Sorting ───────────────────────────────────────────────────────────
        String orderClause = switch (req.getSortBy() == null ? "rel" : req.getSortBy()) {
            case "price_asc" -> "ORDER BY CAST(cv.min_price AS NUMERIC) ASC";
            case "price_desc" -> "ORDER BY CAST(cv.min_price AS NUMERIC) DESC";
            case "rating" -> "ORDER BY p.average_rating DESC NULLS LAST";
            case "newest" -> "ORDER BY p.created_at DESC";
            case "discount" -> """
                    ORDER BY (
                        SELECT CAST(pv3.discount_percentage AS NUMERIC)
                        FROM product_variants pv3
                        WHERE pv3.id = cv.variant_id
                        LIMIT 1
                    ) DESC NULLS LAST""";
            default -> "ORDER BY p.rating_count DESC NULLS LAST, p.average_rating DESC NULLS LAST";
        };
        sql.append(orderClause).append("\n");

        // ── Pagination ────────────────────────────────────────────────────────
        sql.append("LIMIT :limit OFFSET :offset\n");
        params.put("limit", req.getPageSize());
        params.put("offset", (long) req.getPage() * req.getPageSize());

        // ── Execute ───────────────────────────────────────────────────────────
        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows;
    }

    // ─── Count query (for hasMore / totalCount) ───────────────────────────────

    public long count(SearchRequest req) {
        StringBuilder sql = new StringBuilder("""
                WITH cheapest_variant AS (
                    SELECT DISTINCT ON (pv.product_id)
                        pv.id AS variant_id,
                        pv.product_id,
                        pv.price AS min_price,
                        pv.stock
                    FROM product_variants pv
                    WHERE pv.stock > 0
                    ORDER BY pv.product_id, CAST(pv.price AS NUMERIC) ASC
                )
                SELECT COUNT(*)
                FROM products p
                JOIN cheapest_variant cv ON cv.product_id = p.id
                LEFT JOIN brands b       ON b.id = p.brand_id
                WHERE p.step = 'LIVE'
                """);

        Map<String, Object> params = new LinkedHashMap<>();

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            sql.append("""
                    AND (
                        p.name        ILIKE CONCAT('%', :keyword, '%')
                        OR p.description ILIKE CONCAT('%', :keyword, '%')
                        OR b.name     ILIKE CONCAT('%', :keyword, '%')
                    )
                    """);
            params.put("keyword", req.getKeyword().trim());
        }
        if (req.getCategoryId() != null) {
            sql.append("AND p.category_id = :categoryId\n");
            params.put("categoryId", req.getCategoryId());
        }
        if (req.getBrandIds() != null && !req.getBrandIds().isEmpty()) {
            sql.append("AND p.brand_id = ANY(:brandIds)\n");
            params.put("brandIds", req.getBrandIds().toArray(new UUID[0]));
        }
        if (req.getMinPrice() != null) {
            sql.append("AND CAST(cv.min_price AS NUMERIC) >= :minPrice\n");
            params.put("minPrice", req.getMinPrice() * 100);
        }
        if (req.getMaxPrice() != null) {
            sql.append("AND CAST(cv.min_price AS NUMERIC) <= :maxPrice\n");
            params.put("maxPrice", req.getMaxPrice() * 100);
        }
        if (req.getMinRating() != null) {
            sql.append("AND p.average_rating >= :minRating\n");
            params.put("minRating", req.getMinRating());
        }
        if (Boolean.TRUE.equals(req.getTopRated())) {
            sql.append("AND p.average_rating >= 4.0\n");
        }
        if (Boolean.TRUE.equals(req.getNewArrivals())) {
            sql.append("AND p.created_at >= NOW() - INTERVAL '30 days'\n");
        }

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        return ((Number) query.getSingleResult()).longValue();
    }
}

/*
 * ── Recommended PostgreSQL indexes ─────────────────────────────────────
 *
 * -- Partial index: only live products
 * CREATE INDEX idx_products_live ON products(id) WHERE step = 'LIVE';
 *
 * -- For keyword search (trigram)
 * CREATE EXTENSION IF NOT EXISTS pg_trgm;
 * CREATE INDEX idx_products_name_trgm ON products USING gin(name gin_trgm_ops);
 * CREATE INDEX idx_products_desc_trgm ON products USING gin(description
 * gin_trgm_ops);
 *
 * -- For rating-based sorting / filtering
 * CREATE INDEX idx_products_rating ON products(average_rating DESC NULLS LAST)
 * WHERE step = 'LIVE';
 *
 * -- For created_at (new arrivals)
 * CREATE INDEX idx_products_created ON products(created_at DESC)
 * WHERE step = 'LIVE';
 *
 * -- For variant lookup
 * CREATE INDEX idx_variants_product_stock ON product_variants(product_id,
 * stock)
 * WHERE stock > 0;
 *
 * ────────────────────────────────────────────────────────────────────────
 */