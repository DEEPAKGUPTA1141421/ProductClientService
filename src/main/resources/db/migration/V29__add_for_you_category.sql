-- Add "For You" as a category (personalized home feed), and a starter section for it.
-- The sections API accepts either a category UUID (/api/v1/sections/{categoryId}) or,
-- when the path segment isn't a valid UUID, the literal category name — the frontend
-- calls /api/v1/sections/For%20You as the default/guest home feed.

-- category_level is stored ordinal (smallint) by Hibernate since Category.Level
-- has no @Enumerated(STRING): SUPER_CATEGORY=0, CATEGORY=1, SUBCATEGORY=2, SUBSUBCATEGORY=3.
INSERT INTO categories (id, name, priority, category_level, min_products, max_products)
SELECT gen_random_uuid(), 'For You', 0, 1, 1, 9
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'For You');

-- Starter sections for the "For You" feed
INSERT INTO sections (id, title, type, widget_key, data_source, config, position, active, category, starts_at, version)
VALUES
  ('10000000-f0f0-0000-0000-000000000001', 'Recommended for You', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_FOR_YOU', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA","padding":{"x":16,"y":12}}}'::jsonb, 1, true, 'For You', NOW(), 1),
  ('10000000-f0f0-0000-0000-000000000002', 'Trending Now', 'PRODUCT_GRID', 'product_grid_v1', 'RECO_TRENDING', '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFFFFF"}}'::jsonb, 2, true, 'For You', NOW(), 1)
ON CONFLICT (id) DO NOTHING;
