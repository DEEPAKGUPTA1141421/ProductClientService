-- Flipkart/Amazon-style dynamic sections for a pilot set of categories.
--
-- Unlike V22 (STATIC sections needing manually inserted section_items rows),
-- everything here uses CATEGORY_TOP / SEARCH_QUERY data sources so items are
-- resolved live from Elasticsearch (products-v1) via SectionHydrator — no
-- dependency on pre-existing product UUIDs, and results grow with the catalog.
--
-- dataParams shape:
--   CATEGORY_TOP: {"categoryId","sortBy":"rating|discount|newest|popularity","minPricePaise","maxPricePaise","minDiscount","k"}
--   SEARCH_QUERY: {"query","categoryId","sortBy":"rel|price_asc|...","minPricePaise","maxPricePaise","minDiscount","k"}
--
-- Positions continue after the sections V22 already seeded for these categories
-- (which run 1-4), so these appear further down the category page.

-- ============================================================================
-- Men Fashion (0b51af17-cea5-411d-b0b2-d1e86b35c8b0)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000001-0000-0000-0000-000000000001', 'Trendy Picks Under ₹999', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","maxPricePaise":99900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Men Fashion', NOW(), 1),

  ('20000001-0000-0000-0000-000000000002', 'Puma & Top Sports Brands', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"query":"puma","categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Men Fashion', NOW(), 1),

  ('20000001-0000-0000-0000-000000000003', 'Trendy Shirts', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"query":"shirt","categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","sortBy":"popularity","k":16}'::jsonb,
    12, true, 'Men Fashion', NOW(), 1),

  ('20000001-0000-0000-0000-000000000004', 'Min. 40% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF3E0"}}'::jsonb,
    '{"categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","minDiscount":40,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Men Fashion', NOW(), 1),

  ('20000001-0000-0000-0000-000000000005', 'Top Rated Menswear', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","sortBy":"rating","k":16}'::jsonb,
    14, true, 'Men Fashion', NOW(), 1),

  ('20000001-0000-0000-0000-000000000006', 'Just Launched', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#F5F5F5"}}'::jsonb,
    '{"categoryId":"0b51af17-cea5-411d-b0b2-d1e86b35c8b0","sortBy":"newest","k":12}'::jsonb,
    15, true, 'Men Fashion', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Women Fashion (6b525380-eed4-4bb3-b688-5840e49296ee)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000002-0000-0000-0000-000000000001', 'Kurtis Under ₹500', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF5F8","padding":{"x":16,"y":12}}}'::jsonb,
    '{"query":"kurti","categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","maxPricePaise":50000,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Women Fashion', NOW(), 1),

  ('20000002-0000-0000-0000-000000000002', 'New Palazzos & Bottoms', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"query":"palazzo","categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","sortBy":"newest","k":12}'::jsonb,
    11, true, 'Women Fashion', NOW(), 1),

  ('20000002-0000-0000-0000-000000000003', 'Trendy Tops & Shirts', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"query":"top","categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","sortBy":"popularity","k":16}'::jsonb,
    12, true, 'Women Fashion', NOW(), 1),

  ('20000002-0000-0000-0000-000000000004', 'Min. 50% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FCE4EC"}}'::jsonb,
    '{"categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","minDiscount":50,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Women Fashion', NOW(), 1),

  ('20000002-0000-0000-0000-000000000005', 'Top Rated in Women Fashion', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","sortBy":"rating","k":16}'::jsonb,
    14, true, 'Women Fashion', NOW(), 1),

  ('20000002-0000-0000-0000-000000000006', 'Ethnic Wear Edit', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#F5F5F5"}}'::jsonb,
    '{"query":"ethnic","categoryId":"6b525380-eed4-4bb3-b688-5840e49296ee","sortBy":"rel","k":12}'::jsonb,
    15, true, 'Women Fashion', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Mobiles & Tablets (5d70fc95-8a6b-4d04-95e9-9620269ab15e)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000003-0000-0000-0000-000000000001', 'Under ₹15,000', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"5d70fc95-8a6b-4d04-95e9-9620269ab15e","maxPricePaise":1500000,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Mobiles & Tablets', NOW(), 1),

  ('20000003-0000-0000-0000-000000000002', 'Top Rated Smartphones', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F9F9F9"}}'::jsonb,
    '{"categoryId":"5d70fc95-8a6b-4d04-95e9-9620269ab15e","sortBy":"rating","k":12}'::jsonb,
    11, true, 'Mobiles & Tablets', NOW(), 1),

  ('20000003-0000-0000-0000-000000000003', 'Best Deals: Min. 30% Off', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#E3F2FD"}}'::jsonb,
    '{"categoryId":"5d70fc95-8a6b-4d04-95e9-9620269ab15e","minDiscount":30,"sortBy":"discount","k":16}'::jsonb,
    12, true, 'Mobiles & Tablets', NOW(), 1),

  ('20000003-0000-0000-0000-000000000004', 'Latest Launches', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"5d70fc95-8a6b-4d04-95e9-9620269ab15e","sortBy":"newest","k":12}'::jsonb,
    13, true, 'Mobiles & Tablets', NOW(), 1),

  ('20000003-0000-0000-0000-000000000005', 'Tablets for Work & Play', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"query":"tablet","categoryId":"5d70fc95-8a6b-4d04-95e9-9620269ab15e","sortBy":"popularity","k":16}'::jsonb,
    14, true, 'Mobiles & Tablets', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Grocery (b6b5e44d-37bb-4ef9-9b77-26fa8e3836db)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000004-0000-0000-0000-000000000001', 'Everyday Essentials Under ₹99', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":150,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#F1F8E9","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"b6b5e44d-37bb-4ef9-9b77-26fa8e3836db","maxPricePaise":9900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Grocery', NOW(), 1),

  ('20000004-0000-0000-0000-000000000002', 'Best Value Combos', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#FFFDE7"}}'::jsonb,
    '{"categoryId":"b6b5e44d-37bb-4ef9-9b77-26fa8e3836db","minDiscount":20,"sortBy":"discount","k":12}'::jsonb,
    11, true, 'Grocery', NOW(), 1),

  ('20000004-0000-0000-0000-000000000003', 'Fresh Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":150,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"b6b5e44d-37bb-4ef9-9b77-26fa8e3836db","sortBy":"newest","k":16}'::jsonb,
    12, true, 'Grocery', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- Verify with:
-- SELECT category, title, data_source, data_params FROM sections WHERE data_source IN ('CATEGORY_TOP','SEARCH_QUERY') ORDER BY category, position;
