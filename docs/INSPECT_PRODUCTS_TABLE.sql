-- Inspect your products table to populate section_items correctly
-- Run these queries in your database to discover the schema

-- ============================================================================
-- 1. Find product tables
-- ============================================================================
SELECT
  table_name,
  table_schema
FROM information_schema.tables
WHERE table_schema = 'public'
  AND (table_name ILIKE '%product%' OR table_name ILIKE '%item%')
ORDER BY table_name;

-- ============================================================================
-- 2. Inspect the main products table structure
-- ============================================================================
-- Replace 'products' or 'standard_product' with the actual table name from step 1
SELECT
  column_name,
  data_type,
  is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'products'
ORDER BY ordinal_position;

-- ============================================================================
-- 3. Check how many products you have
-- ============================================================================
SELECT COUNT(*) as total_products FROM products;
SELECT COUNT(*) as products_in_live FROM products WHERE step = 'LIVE';
SELECT COUNT(*) as products_in_stock FROM products WHERE in_stock = true;

-- ============================================================================
-- 4. Sample products (for verification)
-- ============================================================================
SELECT
  id,
  name,
  min_price_paise,
  in_stock,
  step
FROM products
LIMIT 10;

-- ============================================================================
-- 5. Once you've verified the structure, use this to populate section_items
-- ============================================================================

-- Insert products into "Trending in Home & Living" section
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1),
  'PRODUCT'::text,
  p.id::text,
  ROW_NUMBER() OVER (ORDER BY p.id) - 1,
  NULL::jsonb
FROM products p
WHERE p.step = 'LIVE' AND p.in_stock = true
LIMIT 6;

-- Insert products into "Best Sellers" section (Mobiles & Tablets)
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  (SELECT id FROM sections WHERE title = 'Best Sellers' AND category = 'Mobiles & Tablets' LIMIT 1),
  'PRODUCT'::text,
  p.id::text,
  ROW_NUMBER() OVER (ORDER BY p.id) - 1,
  NULL::jsonb
FROM products p
WHERE p.step = 'LIVE' AND p.in_stock = true
LIMIT 6;

-- Insert expensive products into "Premium Flagships" section
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  (SELECT id FROM sections WHERE title = 'Premium Flagships' LIMIT 1),
  'PRODUCT'::text,
  p.id::text,
  ROW_NUMBER() OVER (ORDER BY p.min_price_paise DESC) - 1,
  NULL::jsonb
FROM products p
WHERE p.step = 'LIVE' AND p.in_stock = true
ORDER BY p.min_price_paise DESC
LIMIT 4;

-- ============================================================================
-- 6. Insert BANNER items (from sections system)
-- ============================================================================

-- Banner sections are already created in V22. Insert banner items:
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
VALUES
  (
    gen_random_uuid(),
    (SELECT id FROM sections WHERE title = 'Hero Banner' AND category = 'Home & Living' LIMIT 1),
    'BANNER'::text,
    gen_random_uuid()::text,
    0,
    jsonb_build_object(
      'campaignId', 'home_living_hero_2026',
      'imageUrl', 'https://via.placeholder.com/1200x200?text=Home+%26+Living+Hero',
      'altText', 'Curated home solutions for you',
      'tapAction', jsonb_build_object('kind', 'CATEGORY', 'value', 'home-offers'),
      'impressionPixel', 'https://analytics.example.com/i?c=home_hero&t=imp',
      'clickPixel', 'https://analytics.example.com/c?c=home_hero&t=click'
    )
  ),
  (
    gen_random_uuid(),
    (SELECT id FROM sections WHERE title = 'Latest Phones' AND category = 'Mobiles & Tablets' LIMIT 1),
    'BANNER'::text,
    gen_random_uuid()::text,
    0,
    jsonb_build_object(
      'campaignId', 'mobile_latest_2026',
      'imageUrl', 'https://via.placeholder.com/1200x220?text=Latest+Phones',
      'altText', 'New flagship phones now available',
      'tapAction', jsonb_build_object('kind', 'DEEPLINK', 'value', 'app://category/mobiles'),
      'impressionPixel', 'https://analytics.example.com/i?c=mobile_hero&t=imp',
      'clickPixel', 'https://analytics.example.com/c?c=mobile_hero&t=click'
    )
  ),
  (
    gen_random_uuid(),
    (SELECT id FROM sections WHERE title = 'Beauty Essentials' AND category = 'Beauty & Personal Care' LIMIT 1),
    'BANNER'::text,
    gen_random_uuid()::text,
    0,
    jsonb_build_object(
      'campaignId', 'beauty_essentials_2026',
      'imageUrl', 'https://via.placeholder.com/1200x200?text=Beauty+Essentials',
      'altText', 'Premium beauty products curated for you',
      'tapAction', jsonb_build_object('kind', 'SEARCH', 'value', 'beauty-bestsellers'),
      'impressionPixel', 'https://analytics.example.com/i?c=beauty_hero&t=imp',
      'clickPixel', 'https://analytics.example.com/c?c=beauty_hero&t=click'
    )
  );

-- ============================================================================
-- 7. Verify items were inserted
-- ============================================================================
SELECT
  s.title,
  s.category,
  COUNT(si.id) as item_count,
  STRING_AGG(DISTINCT si.item_type, ', ') as item_types
FROM sections s
LEFT JOIN section_items si ON s.id = si.section_id
GROUP BY s.id, s.title, s.category
ORDER BY s.position;
