# Sections & Items Seed Data Guide

This document provides SQL to delete old section data and seed realistic sections for all categories.

---

## Step 1: Delete existing data (WARNING: irreversible)

```sql
-- Delete all section items first (foreign key constraint)
TRUNCATE TABLE section_items CASCADE;

-- Delete all sections
TRUNCATE TABLE sections CASCADE;

-- Reset sequences (optional, for auto-increment clarity)
ALTER SEQUENCE sections_id_seq RESTART WITH 1;
ALTER SEQUENCE section_items_id_seq RESTART WITH 1;
```

---

## Step 2: Seed sections for all categories

Insert sections with various layouts. Each category gets 4–5 sections with different `widgetKey` values to showcase the backend-driven UI system.

```sql
-- ============================================================================
-- Home & Living (1660f3b3-e366-4561-a479-7b9fc9f3ac26)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Hero Banner',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":200,"aspectRatio":"16:7","autoplay":false,"theme":{"bg":"#FFF","fg":"#333","fontWeight":"bold"}}'::jsonb,
    1,
    true,
    'Home & Living',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Trending in Home & Living',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"imageAspectRatio":"1:1","theme":{"bg":"#F5F5F5","padding":{"x":16,"y":12}}}'::jsonb,
    2,
    true,
    'Home & Living',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Premium Picks',
    'PRODUCT_HIGHLIGHT',
    'product_highlight_v1',
    'STATIC',
    '{"columns":2,"rows":2,"cardVariant":"highlight","imageAspectRatio":"4:5","showPriceStrikethrough":true,"ctaText":"Shop now","theme":{"bg":"#E8F4F8","fontWeight":"bold","padding":{"x":20,"y":20}}}'::jsonb,
    3,
    true,
    'Home & Living',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Top Brands',
    'BRAND',
    'brand_spotlight_v1',
    'STATIC',
    '{"itemWidth":100,"shape":"circle","showName":true,"maxItems":12,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    4,
    true,
    'Home & Living',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'New Arrivals',
    'PRODUCT_SCROLL',
    'product_scroll_v1',
    'STATIC',
    '{"itemWidth":160,"cardVariant":"compact","showDiscount":true,"showRating":true,"peekNext":true,"snap":"start","maxItems":20,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    5,
    true,
    'Home & Living',
    NOW(),
    1
  );

-- ============================================================================
-- Mobiles & Tablets (5d70fc95-8a6b-4d04-95e9-9620269ab15e)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Latest Phones',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":220,"aspectRatio":"16:6","autoplay":true,"loopMs":5000,"theme":{"bg":"#1a1a1a","fg":"#FFF","accent":"#FF6B35"}}'::jsonb,
    1,
    true,
    'Mobiles & Tablets',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Best Sellers',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_FOR_YOU',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F9F9F9","padding":{"x":12,"y":12}}}'::jsonb,
    2,
    true,
    'Mobiles & Tablets',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Premium Flagships',
    'PRODUCT_HIGHLIGHT',
    'product_highlight_v1',
    'STATIC',
    '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","showPriceStrikethrough":true,"ctaText":"View Details","theme":{"bg":"#E3F2FD","fontWeight":"bold"}}'::jsonb,
    3,
    true,
    'Mobiles & Tablets',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Popular Brands',
    'BRAND',
    'brand_spotlight_v1',
    'STATIC',
    '{"itemWidth":90,"shape":"square","showName":true,"maxItems":10,"theme":{"bg":"#FFF"}}'::jsonb,
    4,
    true,
    'Mobiles & Tablets',
    NOW(),
    1
  );

-- ============================================================================
-- Consumer Electronics (3f6bf59e-66e6-4cd3-abdb-2780f608f052)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Great Tech Deals',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#0F0F23","fg":"#FFF","accent":"#00D4FF"}}'::jsonb,
    1,
    true,
    'Consumer Electronics',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Featured Electronics',
    'PRODUCT_SCROLL',
    'product_scroll_v1',
    'STATIC',
    '{"itemWidth":180,"cardVariant":"compact","showDiscount":true,"peekNext":true,"maxItems":25,"theme":{"bg":"#FFF","padding":{"x":16,"y":12}}}'::jsonb,
    2,
    true,
    'Consumer Electronics',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Bestselling Gadgets',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    3,
    true,
    'Consumer Electronics',
    NOW(),
    1
  );

-- ============================================================================
-- Beauty & Personal Care (f7aeabba-0dd4-4545-b855-402d58d04e85)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Beauty Essentials',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#FFE5E5","fg":"#333","accent":"#FF69B4"}}'::jsonb,
    1,
    true,
    'Beauty & Personal Care',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Top Rated Products',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF8F9","padding":{"x":16,"y":12}}}'::jsonb,
    2,
    true,
    'Beauty & Personal Care',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Premium Skincare',
    'PRODUCT_HIGHLIGHT',
    'product_highlight_v1',
    'STATIC',
    '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","ctaText":"Buy Now","theme":{"bg":"#FAFAFA","fontWeight":"bold"}}'::jsonb,
    3,
    true,
    'Beauty & Personal Care',
    NOW(),
    1
  );

-- ============================================================================
-- Sports & Fitness (f6beded0-0ebb-414c-9ec2-a7415a5a5e24)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Fitness Gear',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#1B5E20","fg":"#FFF","accent":"#4CAF50"}}'::jsonb,
    1,
    true,
    'Sports & Fitness',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Trending Equipment',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#F1F8E9"}}'::jsonb,
    2,
    true,
    'Sports & Fitness',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'New Arrivals',
    'PRODUCT_SCROLL',
    'product_scroll_v1',
    'STATIC',
    '{"itemWidth":170,"cardVariant":"compact","peekNext":true,"maxItems":20,"theme":{"bg":"#FFF"}}'::jsonb,
    3,
    true,
    'Sports & Fitness',
    NOW(),
    1
  );

-- ============================================================================
-- Men Fashion (0b51af17-cea5-411d-b0b2-d1e86b35c8b0)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Men\'s Collection',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":220,"aspectRatio":"16:7","theme":{"bg":"#263238","fg":"#FFF","accent":"#FFD700"}}'::jsonb,
    1,
    true,
    'Men Fashion',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Top Trending',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_FOR_YOU',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    2,
    true,
    'Men Fashion',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Premium Selection',
    'PRODUCT_HIGHLIGHT',
    'product_highlight_v1',
    'STATIC',
    '{"columns":2,"cardVariant":"highlight","imageAspectRatio":"4:5","theme":{"bg":"#F5F5F5","fontWeight":"bold"}}'::jsonb,
    3,
    true,
    'Men Fashion',
    NOW(),
    1
  );

-- ============================================================================
-- Women Fashion (6b525380-eed4-4bb3-b688-5840e49296ee)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Women\'s Fashion',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":220,"aspectRatio":"16:7","theme":{"bg":"#880E4F","fg":"#FFF","accent":"#FF1744"}}'::jsonb,
    1,
    true,
    'Women Fashion',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Trending Now',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFF5F8"}}'::jsonb,
    2,
    true,
    'Women Fashion',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Exclusive Styles',
    'PRODUCT_SCROLL',
    'product_scroll_v1',
    'STATIC',
    '{"itemWidth":160,"cardVariant":"compact","peekNext":true,"maxItems":25,"theme":{"bg":"#FFF"}}'::jsonb,
    3,
    true,
    'Women Fashion',
    NOW(),
    1
  );

-- ============================================================================
-- Grocery (b6b5e44d-37bb-4ef9-9b77-26fa8e3836db)
-- ============================================================================

INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  (
    gen_random_uuid(),
    'Fresh & Organic',
    'BANNER',
    'banner_hero_v1',
    'STATIC',
    '{"height":200,"aspectRatio":"16:7","theme":{"bg":"#E8F5E9","fg":"#2E7D32","accent":"#66BB6A"}}'::jsonb,
    1,
    true,
    'Grocery',
    NOW(),
    1
  ),
  (
    gen_random_uuid(),
    'Popular Items',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"theme":{"bg":"#F1F1F1"}}'::jsonb,
    2,
    true,
    'Grocery',
    NOW(),
    1
  );

-- ============================================================================
-- Additional categories follow the same pattern
-- Add similar sections for remaining categories: Appliances, Kids & Toys, Books, etc.
-- ============================================================================
```

---

## Step 3: Insert sample section items (products)

**First, check your actual product table structure:**

```sql
-- List tables and find your products table
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name ILIKE '%product%';

-- Check columns in products table
\d products
```

**Safe approach: Insert product items using actual product IDs**

```sql
-- For any section, link actual products
-- Replace product UUIDs with real IDs from your database

INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
VALUES
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-1', 0, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-2', 1, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-3', 2, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-4', 3, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-5', 4, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, 'UUID-6', 5, NULL::jsonb);
```

**OR: Dynamically fetch products (if your product table has matching structure):**

```sql
-- First find the actual table name and column names in your DB
-- Then use this pattern:

INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT 
  gen_random_uuid(),
  (SELECT id FROM sections WHERE title = 'Best Sellers' LIMIT 1),
  'PRODUCT'::text,
  p.id::text,
  ROW_NUMBER() OVER (ORDER BY p.id) - 1,
  NULL::jsonb
FROM products p
LIMIT 6;
```

---

## Alternative: Simpler approach with mock data

If your product table is empty or you want to test with static items:

```sql
-- Insert banner items for hero sections
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT 
  gen_random_uuid(),
  s.id,
  'BANNER'::text,
  gen_random_uuid()::text,
  0,
  jsonb_build_object(
    'campaignId', 'campaign_' || s.title,
    'imageUrl', 'https://via.placeholder.com/1200x200?text=' || s.title,
    'altText', s.title,
    'tapAction', jsonb_build_object('kind', 'CATEGORY', 'value', s.category)
  )
FROM sections s
WHERE s.type = 'BANNER'
LIMIT 1;
```

---

## Step 4: Verify data

```sql
-- Check sections count
SELECT category, COUNT(*) as section_count 
FROM sections 
GROUP BY category 
ORDER BY section_count DESC;

-- Check items count per section
SELECT s.title, COUNT(si.id) as item_count 
FROM sections s
LEFT JOIN section_items si ON s.id = si.section_id
GROUP BY s.id, s.title
ORDER BY s.position;

-- View a section with all its details
SELECT 
  s.id,
  s.title,
  s.widget_key,
  s.data_source,
  s.config,
  COUNT(si.id) as item_count
FROM sections s
LEFT JOIN section_items si ON s.id = si.section_id
WHERE s.category = 'Home & Living'
GROUP BY s.id
ORDER BY s.position;
```

---

## Notes

- **widgetKey** determines the frontend component (`product_grid_v1`, `banner_hero_v1`, etc.)
- **dataSource** can be `STATIC` (from section_items), `RECO_FOR_YOU` (personalized), `RECO_TRENDING`, etc.
- **config** is a JSONB object that controls layout (columns, rows, theme colors, padding)
- **position** orders sections top-to-bottom on the page
- All sections are `active=true` and `starts_at=NOW()` (visible immediately)
- Adjust product IDs to match your actual product IDs in the database
