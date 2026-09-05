-- Flipkart/Amazon-style dynamic sections for the remaining 8 categories
-- (Men Fashion, Women Fashion, Mobiles & Tablets, Grocery were done in V30).
--
-- Same pattern as V30: CATEGORY_TOP / SEARCH_QUERY data sources, resolved live
-- from Elasticsearch by SectionHydrator — no dependency on pre-existing
-- product UUIDs. Positions continue after each category's V22 static sections.

-- ============================================================================
-- Home & Living (1660f3b3-e366-4561-a479-7b9fc9f3ac26)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000005-0000-0000-0000-000000000001', 'Home Decor Under ₹499', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    '{"query":"decor","categoryId":"1660f3b3-e366-4561-a479-7b9fc9f3ac26","maxPricePaise":49900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Home & Living', NOW(), 1),

  ('20000005-0000-0000-0000-000000000002', 'Kitchen & Dining Essentials', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"query":"kitchen","categoryId":"1660f3b3-e366-4561-a479-7b9fc9f3ac26","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Home & Living', NOW(), 1),

  ('20000005-0000-0000-0000-000000000003', 'Min. 30% Off', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#E8F4F8"}}'::jsonb,
    '{"categoryId":"1660f3b3-e366-4561-a479-7b9fc9f3ac26","minDiscount":30,"sortBy":"discount","k":16}'::jsonb,
    12, true, 'Home & Living', NOW(), 1),

  ('20000005-0000-0000-0000-000000000004', 'Top Rated Home Picks', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F5F5F5"}}'::jsonb,
    '{"categoryId":"1660f3b3-e366-4561-a479-7b9fc9f3ac26","sortBy":"rating","k":12}'::jsonb,
    13, true, 'Home & Living', NOW(), 1),

  ('20000005-0000-0000-0000-000000000005', 'New in Home & Living', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"1660f3b3-e366-4561-a479-7b9fc9f3ac26","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Home & Living', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Kids & Toys (5c46c5f5-04b9-47c0-b20c-3bd108a72c14)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000006-0000-0000-0000-000000000001', 'Toys Under ₹299', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":150,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF9E6"}}'::jsonb,
    '{"categoryId":"5c46c5f5-04b9-47c0-b20c-3bd108a72c14","maxPricePaise":29900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Kids & Toys', NOW(), 1),

  ('20000006-0000-0000-0000-000000000002', 'Educational & Learning Toys', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"query":"educational","categoryId":"5c46c5f5-04b9-47c0-b20c-3bd108a72c14","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Kids & Toys', NOW(), 1),

  ('20000006-0000-0000-0000-000000000003', 'Top Rated Toys', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":150,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"5c46c5f5-04b9-47c0-b20c-3bd108a72c14","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Kids & Toys', NOW(), 1),

  ('20000006-0000-0000-0000-000000000004', 'Best Deals: Min. 25% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#FFE082"}}'::jsonb,
    '{"categoryId":"5c46c5f5-04b9-47c0-b20c-3bd108a72c14","minDiscount":25,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Kids & Toys', NOW(), 1),

  ('20000006-0000-0000-0000-000000000005', 'New Toy Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":150,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"5c46c5f5-04b9-47c0-b20c-3bd108a72c14","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Kids & Toys', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Consumer Electronics (3f6bf59e-66e6-4cd3-abdb-2780f608f052)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000007-0000-0000-0000-000000000001', 'Under ₹999 Gadgets', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#0F0F23","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"3f6bf59e-66e6-4cd3-abdb-2780f608f052","maxPricePaise":99900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Consumer Electronics', NOW(), 1),

  ('20000007-0000-0000-0000-000000000002', 'Headphones & Earbuds', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"query":"headphone","categoryId":"3f6bf59e-66e6-4cd3-abdb-2780f608f052","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Consumer Electronics', NOW(), 1),

  ('20000007-0000-0000-0000-000000000003', 'Top Rated Electronics', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"3f6bf59e-66e6-4cd3-abdb-2780f608f052","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Consumer Electronics', NOW(), 1),

  ('20000007-0000-0000-0000-000000000004', 'Min. 30% Off Deals', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#E0F7FA"}}'::jsonb,
    '{"categoryId":"3f6bf59e-66e6-4cd3-abdb-2780f608f052","minDiscount":30,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Consumer Electronics', NOW(), 1),

  ('20000007-0000-0000-0000-000000000005', 'Just Launched', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"3f6bf59e-66e6-4cd3-abdb-2780f608f052","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Consumer Electronics', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Appliances (91c0ef20-1199-48a8-bec8-208e5d04b15e)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000008-0000-0000-0000-000000000001', 'Small Appliances Under ₹999', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#F1F8E9","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"91c0ef20-1199-48a8-bec8-208e5d04b15e","maxPricePaise":99900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Appliances', NOW(), 1),

  ('20000008-0000-0000-0000-000000000002', 'Top Rated Appliances', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#ECEFF1"}}'::jsonb,
    '{"categoryId":"91c0ef20-1199-48a8-bec8-208e5d04b15e","sortBy":"rating","k":12}'::jsonb,
    11, true, 'Appliances', NOW(), 1),

  ('20000008-0000-0000-0000-000000000003', 'Min. 20% Off', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#C3E7FF"}}'::jsonb,
    '{"categoryId":"91c0ef20-1199-48a8-bec8-208e5d04b15e","minDiscount":20,"sortBy":"discount","k":16}'::jsonb,
    12, true, 'Appliances', NOW(), 1),

  ('20000008-0000-0000-0000-000000000004', 'New Arrivals', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"91c0ef20-1199-48a8-bec8-208e5d04b15e","sortBy":"newest","k":12}'::jsonb,
    13, true, 'Appliances', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Beauty & Personal Care (f7aeabba-0dd4-4545-b855-402d58d04e85)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('20000009-0000-0000-0000-000000000001', 'Skincare Under ₹299', 'PRODUCT_SCROLL', 'product_scroll_v1', 'SEARCH_QUERY',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFE5E5","padding":{"x":16,"y":12}}}'::jsonb,
    '{"query":"skincare","categoryId":"f7aeabba-0dd4-4545-b855-402d58d04e85","maxPricePaise":29900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Beauty & Personal Care', NOW(), 1),

  ('20000009-0000-0000-0000-000000000002', 'Makeup Must-Haves', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF8F9"}}'::jsonb,
    '{"query":"makeup","categoryId":"f7aeabba-0dd4-4545-b855-402d58d04e85","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Beauty & Personal Care', NOW(), 1),

  ('20000009-0000-0000-0000-000000000003', 'Top Rated Beauty', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"f7aeabba-0dd4-4545-b855-402d58d04e85","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Beauty & Personal Care', NOW(), 1),

  ('20000009-0000-0000-0000-000000000004', 'Min. 30% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#FCE4EC"}}'::jsonb,
    '{"categoryId":"f7aeabba-0dd4-4545-b855-402d58d04e85","minDiscount":30,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Beauty & Personal Care', NOW(), 1),

  ('20000009-0000-0000-0000-000000000005', 'New Launches', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"f7aeabba-0dd4-4545-b855-402d58d04e85","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Beauty & Personal Care', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Sports & Fitness (f6beded0-0ebb-414c-9ec2-a7415a5a5e24)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('2000000a-0000-0000-0000-000000000001', 'Fitness Gear Under ₹999', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#1B5E20","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"f6beded0-0ebb-414c-9ec2-a7415a5a5e24","maxPricePaise":99900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Sports & Fitness', NOW(), 1),

  ('2000000a-0000-0000-0000-000000000002', 'Yoga & Home Workout', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F1F8E9"}}'::jsonb,
    '{"query":"yoga","categoryId":"f6beded0-0ebb-414c-9ec2-a7415a5a5e24","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Sports & Fitness', NOW(), 1),

  ('2000000a-0000-0000-0000-000000000003', 'Top Rated Sports Gear', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"f6beded0-0ebb-414c-9ec2-a7415a5a5e24","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Sports & Fitness', NOW(), 1),

  ('2000000a-0000-0000-0000-000000000004', 'Min. 25% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#E8F5E9"}}'::jsonb,
    '{"categoryId":"f6beded0-0ebb-414c-9ec2-a7415a5a5e24","minDiscount":25,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Sports & Fitness', NOW(), 1),

  ('2000000a-0000-0000-0000-000000000005', 'New Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"f6beded0-0ebb-414c-9ec2-a7415a5a5e24","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Sports & Fitness', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Books (f87d53a4-9198-4678-b9e2-a7e42894fe5b)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('2000000b-0000-0000-0000-000000000001', 'Bestsellers Under ₹199', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":140,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#E1BEE7","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"f87d53a4-9198-4678-b9e2-a7e42894fe5b","maxPricePaise":19900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Books', NOW(), 1),

  ('2000000b-0000-0000-0000-000000000002', 'Fiction & Novels', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F3E5F5"}}'::jsonb,
    '{"query":"fiction","categoryId":"f87d53a4-9198-4678-b9e2-a7e42894fe5b","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Books', NOW(), 1),

  ('2000000b-0000-0000-0000-000000000003', 'Top Rated Reads', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":140,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"f87d53a4-9198-4678-b9e2-a7e42894fe5b","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Books', NOW(), 1),

  ('2000000b-0000-0000-0000-000000000004', 'Min. 30% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#F3E5F5"}}'::jsonb,
    '{"categoryId":"f87d53a4-9198-4678-b9e2-a7e42894fe5b","minDiscount":30,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Books', NOW(), 1),

  ('2000000b-0000-0000-0000-000000000005', 'New Releases', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":140,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"f87d53a4-9198-4678-b9e2-a7e42894fe5b","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Books', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- Health & Wellness (1c5aeb0a-5a17-41c9-baac-0d7280483c06)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  ('2000000c-0000-0000-0000-000000000001', 'Wellness Essentials Under ₹499', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#E0F2F1","padding":{"x":16,"y":12}}}'::jsonb,
    '{"categoryId":"1c5aeb0a-5a17-41c9-baac-0d7280483c06","maxPricePaise":49900,"sortBy":"popularity","k":16}'::jsonb,
    10, true, 'Health & Wellness', NOW(), 1),

  ('2000000c-0000-0000-0000-000000000002', 'Supplements & Nutrition', 'PRODUCT_GRID', 'product_grid_v1', 'SEARCH_QUERY',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F0F9F8"}}'::jsonb,
    '{"query":"supplement","categoryId":"1c5aeb0a-5a17-41c9-baac-0d7280483c06","sortBy":"rel","k":12}'::jsonb,
    11, true, 'Health & Wellness', NOW(), 1),

  ('2000000c-0000-0000-0000-000000000003', 'Top Rated Wellness', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FFF"}}'::jsonb,
    '{"categoryId":"1c5aeb0a-5a17-41c9-baac-0d7280483c06","sortBy":"rating","k":16}'::jsonb,
    12, true, 'Health & Wellness', NOW(), 1),

  ('2000000c-0000-0000-0000-000000000004', 'Min. 25% Off', 'PRODUCT_GRID', 'product_grid_v1', 'CATEGORY_TOP',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#E0F2F1"}}'::jsonb,
    '{"categoryId":"1c5aeb0a-5a17-41c9-baac-0d7280483c06","minDiscount":25,"sortBy":"discount","k":12}'::jsonb,
    13, true, 'Health & Wellness', NOW(), 1),

  ('2000000c-0000-0000-0000-000000000005', 'New Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'CATEGORY_TOP',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":16,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"categoryId":"1c5aeb0a-5a17-41c9-baac-0d7280483c06","sortBy":"newest","k":16}'::jsonb,
    14, true, 'Health & Wellness', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- Verify with:
-- SELECT category, title, data_source, data_params FROM sections WHERE data_source IN ('CATEGORY_TOP','SEARCH_QUERY') ORDER BY category, position;
