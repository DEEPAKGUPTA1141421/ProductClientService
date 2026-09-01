-- Seller dashboard: indexes to support seller-scoped product aggregation queries
-- (countAllProductsBySeller, countLiveProductsBySeller, countActiveLiveProductsBySeller,
--  countOutOfStockProductsBySeller, countLowStockProductsBySeller, findLiveProductsBySeller,
--  findLowStockProductsBySeller, findDistinctCategoriesBySeller). These previously had no
-- supporting index beyond the implicit PK/FK indexes, forcing a full scan of `products` per seller.
CREATE INDEX IF NOT EXISTS idx_products_seller_step_active
    ON products (seller_id, step, is_active);

-- product_variants.product_id had no index despite being the join column for every
-- per-product stock aggregation (SUM(v.stock) GROUP BY p.id).
CREATE INDEX IF NOT EXISTS idx_product_variants_product_id
    ON product_variants (product_id);
