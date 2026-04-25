-- Seed sections and items data for all categories
-- This migration populates the sections system with realistic layouts for the app

-- Delete existing data (if re-running)
DELETE FROM section_items;
DELETE FROM sections;

-- ============================================================================
-- Home & Living (1660f3b3-e366-4561-a479-7b9fc9f3ac26)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000001-0000-0000-0000-000000000001', 'Hero Banner', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","autoplay":false,"theme":{"bg":"#FFF","fg":"#333","fontWeight":"bold"}}'::jsonb, 1, true, 'Home & Living', NOW(), 1),
  ('10000001-0000-0000-0000-000000000002', 'Trending in Home & Living', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"imageAspectRatio":"1:1","theme":{"bg":"#F5F5F5","padding":{"x":16,"y":12}}}'::jsonb, 2, true, 'Home & Living', NOW(), 1),
  ('10000001-0000-0000-0000-000000000003', 'Premium Picks', 'PRODUCT_HIGHLIGHT', 'product_highlight_v1', 'STATIC', '{"columns":2,"rows":2,"cardVariant":"highlight","imageAspectRatio":"4:5","showPriceStrikethrough":true,"ctaText":"Shop now","theme":{"bg":"#E8F4F8","fontWeight":"bold","padding":{"x":20,"y":20}}}'::jsonb, 3, true, 'Home & Living', NOW(), 1),
  ('10000001-0000-0000-0000-000000000004', 'Top Brands', 'BRAND', 'brand_spotlight_v1', 'STATIC', '{"itemWidth":100,"shape":"circle","showName":true,"maxItems":12,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb, 4, true, 'Home & Living', NOW(), 1),
  ('10000001-0000-0000-0000-000000000005', 'New Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'STATIC', '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"showRating":true,"peekNext":true,"snap":"start","maxItems":20,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb, 5, true, 'Home & Living', NOW(), 1);

-- ============================================================================
-- Kids & Toys (5c46c5f5-04b9-47c0-b20c-3bd108a72c14)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000002-0000-0000-0000-000000000001', 'Kids Fun Zone', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#FFE082","fg":"#333","accent":"#FF6F00"}}'::jsonb, 1, true, 'Kids & Toys', NOW(), 1),
  ('10000002-0000-0000-0000-000000000002', 'Popular Toys', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF9E6"}}'::jsonb, 2, true, 'Kids & Toys', NOW(), 1),
  ('10000002-0000-0000-0000-000000000003', 'Best Sellers', 'PRODUCT_SCROLL', 'product_scroll_v1', 'STATIC', '{"itemWidth":160,"cardVariant":"compact","peekNext":true,"theme":{"bg":"#FFF"}}'::jsonb, 3, true, 'Kids & Toys', NOW(), 1);

-- ============================================================================
-- Mobiles & Tablets (5d70fc95-8a6b-4d04-95e9-9620269ab15e)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000003-0000-0000-0000-000000000001', 'Latest Phones', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":220,"aspectRatio":"16:6","autoplay":true,"loopMs":5000,"theme":{"bg":"#1a1a1a","fg":"#FFF","accent":"#FF6B35"}}'::jsonb, 1, true, 'Mobiles & Tablets', NOW(), 1),
  ('10000003-0000-0000-0000-000000000002', 'Best Sellers', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_FOR_YOU', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F9F9F9","padding":{"x":12,"y":12}}}'::jsonb, 2, true, 'Mobiles & Tablets', NOW(), 1),
  ('10000003-0000-0000-0000-000000000003', 'Premium Flagships', 'PRODUCT_HIGHLIGHT', 'product_highlight_v1', 'STATIC', '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","showPriceStrikethrough":true,"ctaText":"View Details","theme":{"bg":"#E3F2FD","fontWeight":"bold"}}'::jsonb, 3, true, 'Mobiles & Tablets', NOW(), 1),
  ('10000003-0000-0000-0000-000000000004', 'Popular Brands', 'BRAND', 'brand_spotlight_v1', 'STATIC', '{"itemWidth":90,"shape":"square","showName":true,"maxItems":10,"theme":{"bg":"#FFF"}}'::jsonb, 4, true, 'Mobiles & Tablets', NOW(), 1);

-- ============================================================================
-- Consumer Electronics (3f6bf59e-66e6-4cd3-abdb-2780f608f052)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000004-0000-0000-0000-000000000001', 'Great Tech Deals', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#0F0F23","fg":"#FFF","accent":"#00D4FF"}}'::jsonb, 1, true, 'Consumer Electronics', NOW(), 1),
  ('10000004-0000-0000-0000-000000000002', 'Featured Electronics', 'PRODUCT_SCROLL', 'product_scroll_v1', 'STATIC', '{"itemWidth":180,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":25,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb, 2, true, 'Consumer Electronics', NOW(), 1),
  ('10000004-0000-0000-0000-000000000003', 'Bestselling Gadgets', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb, 3, true, 'Consumer Electronics', NOW(), 1);

-- ============================================================================
-- Appliances (91c0ef20-1199-48a8-bec8-208e5d04b15e)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000005-0000-0000-0000-000000000001', 'Smart Home Appliances', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#C3E7FF","fg":"#0D47A1","accent":"#2196F3"}}'::jsonb, 1, true, 'Appliances', NOW(), 1),
  ('10000005-0000-0000-0000-000000000002', 'Top Rated', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#ECEFF1"}}'::jsonb, 2, true, 'Appliances', NOW(), 1);

-- ============================================================================
-- Beauty & Personal Care (f7aeabba-0dd4-4545-b855-402d58d04e85)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000006-0000-0000-0000-000000000001', 'Beauty Essentials', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#FFE5E5","fg":"#333","accent":"#FF69B4"}}'::jsonb, 1, true, 'Beauty & Personal Care', NOW(), 1),
  ('10000006-0000-0000-0000-000000000002', 'Top Rated Products', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF8F9","padding":{"x":16,"y":12}}}'::jsonb, 2, true, 'Beauty & Personal Care', NOW(), 1),
  ('10000006-0000-0000-0000-000000000003', 'Premium Skincare', 'PRODUCT_HIGHLIGHT', 'product_highlight_v1', 'STATIC', '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","ctaText":"Buy Now","theme":{"bg":"#FAFAFA","fontWeight":"bold"}}'::jsonb, 3, true, 'Beauty & Personal Care', NOW(), 1);

-- ============================================================================
-- Sports & Fitness (f6beded0-0ebb-414c-9ec2-a7415a5a5e24)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000007-0000-0000-0000-000000000001', 'Fitness Gear', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#1B5E20","fg":"#FFF","accent":"#4CAF50"}}'::jsonb, 1, true, 'Sports & Fitness', NOW(), 1),
  ('10000007-0000-0000-0000-000000000002', 'Trending Equipment', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F1F8E9"}}'::jsonb, 2, true, 'Sports & Fitness', NOW(), 1),
  ('10000007-0000-0000-0000-000000000003', 'New Arrivals', 'PRODUCT_SCROLL', 'product_scroll_v1', 'STATIC', '{"itemWidth":170,"cardVariant":"compact","peekNext":true,"maxItems":20,"theme":{"bg":"#FFF"}}'::jsonb, 3, true, 'Sports & Fitness', NOW(), 1);

-- ============================================================================
-- Men Fashion (0b51af17-cea5-411d-b0b2-d1e86b35c8b0)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000008-0000-0000-0000-000000000001', 'Men\'s Collection', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":220,"aspectRatio":"16:7","theme":{"bg":"#263238","fg":"#FFF","accent":"#FFD700"}}'::jsonb, 1, true, 'Men Fashion', NOW(), 1),
  ('10000008-0000-0000-0000-000000000002', 'Top Trending', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_FOR_YOU', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb, 2, true, 'Men Fashion', NOW(), 1),
  ('10000008-0000-0000-0000-000000000003', 'Premium Selection', 'PRODUCT_HIGHLIGHT', 'product_highlight_v1', 'STATIC', '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","theme":{"bg":"#F5F5F5","fontWeight":"bold"}}'::jsonb, 3, true, 'Men Fashion', NOW(), 1);

-- ============================================================================
-- Women Fashion (6b525380-eed4-4bb3-b688-5840e49296ee)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000009-0000-0000-0000-000000000001', 'Women\'s Fashion', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":220,"aspectRatio":"16:7","theme":{"bg":"#880E4F","fg":"#FFF","accent":"#FF1744"}}'::jsonb, 1, true, 'Women Fashion', NOW(), 1),
  ('10000009-0000-0000-0000-000000000002', 'Trending Now', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF5F8"}}'::jsonb, 2, true, 'Women Fashion', NOW(), 1),
  ('10000009-0000-0000-0000-000000000003', 'Exclusive Styles', 'PRODUCT_SCROLL', 'product_scroll_v1', 'STATIC', '{"itemWidth":160,"cardVariant":"compact","peekNext":true,"maxItems":25,"theme":{"bg":"#FFF"}}'::jsonb, 3, true, 'Women Fashion', NOW(), 1);

-- ============================================================================
-- Grocery (b6b5e44d-37bb-4ef9-9b77-26fa8e3836db)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000010-0000-0000-0000-000000000001', 'Fresh & Organic', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#E8F5E9","fg":"#2E7D32","accent":"#66BB6A"}}'::jsonb, 1, true, 'Grocery', NOW(), 1),
  ('10000010-0000-0000-0000-000000000002', 'Popular Items', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#F1F1F1"}}'::jsonb, 2, true, 'Grocery', NOW(), 1);

-- ============================================================================
-- Books (f87d53a4-9198-4678-b9e2-a7e42894fe5b)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000011-0000-0000-0000-000000000001', 'Bestselling Books', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#E1BEE7","fg":"#4A148C","accent":"#7B1FA2"}}'::jsonb, 1, true, 'Books', NOW(), 1),
  ('10000011-0000-0000-0000-000000000002', 'Trending Reads', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F3E5F5"}}'::jsonb, 2, true, 'Books', NOW(), 1);

-- ============================================================================
-- Health & Wellness (1c5aeb0a-5a17-41c9-baac-0d7280483c06)
-- ============================================================================
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000012-0000-0000-0000-000000000001', 'Health First', 'BANNER', 'banner_hero_v1', 'STATIC', '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#E0F2F1","fg":"#00695C","accent":"#00897B"}}'::jsonb, 1, true, 'Health & Wellness', NOW(), 1),
  ('10000012-0000-0000-0000-000000000002', 'Top Wellness Products', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F0F9F8"}}'::jsonb, 2, true, 'Health & Wellness', NOW(), 1);

-- ============================================================================
-- Notes
-- ============================================================================
-- This migration creates SECTIONS only. To populate ITEMS, run the queries in
-- docs/SEED_SECTIONS_DATA.md after verifying your product table structure.
--
-- Verify sections were created:
-- SELECT category, COUNT(*) as section_count FROM sections GROUP BY category ORDER BY section_count DESC;
