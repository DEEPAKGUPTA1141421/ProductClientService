-- Populates section_items for STATIC PRODUCT_GRID / PRODUCT_SCROLL / PRODUCT_HIGHLIGHT
-- / BRAND sections that currently have zero items (V22 only ever created the
-- sections themselves — see docs/SEED_SECTIONS_DATA.md). Pulls real rows from
-- `products` / `brands` for that section's own category, so no fake UUIDs.
--
-- Safe to re-run: a section is only touched if it has no section_items yet.
--
-- Note: Product.step has no @Enumerated(STRING), so Hibernate stores it
-- ordinally — LIVE is ordinal 5 (PRODUCT_NAME=0, PRODUCT_ATTRIBUTE=1,
-- PRODUCT_VARIANT=2, PRODUCT_IMAGE=3, PRODUCT_BRAND_AND_TAGS=4, LIVE=5).
-- Section.type / Section.dataSource / SectionItem.itemType ARE
-- @Enumerated(STRING), so those compare as plain text.

-- ============================================================================
-- PRODUCT_GRID / PRODUCT_SCROLL / PRODUCT_HIGHLIGHT (dataSource STATIC)
-- ============================================================================
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  s.id,
  'PRODUCT',
  p.id::text,
  (ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY p.created_at DESC) - 1)::int,
  NULL
FROM sections s
JOIN categories c ON c.name = s.category
JOIN LATERAL (
  SELECT pr.id, pr.created_at
  FROM products pr
  WHERE pr.category_id = c.id
    AND pr.step = 5            -- LIVE
    AND pr.is_active = true
  ORDER BY pr.created_at DESC
  LIMIT 12
) p ON true
WHERE s.data_source = 'STATIC'
  AND s.type IN ('PRODUCT_GRID', 'PRODUCT_SCROLL', 'PRODUCT_HIGHLIGHT')
  AND NOT EXISTS (SELECT 1 FROM section_items si WHERE si.section_id = s.id);

-- ============================================================================
-- BRAND sections (dataSource STATIC)
-- ============================================================================
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  s.id,
  'BRAND',
  b.id::text,
  (ROW_NUMBER() OVER (PARTITION BY s.id ORDER BY b.name) - 1)::int,
  jsonb_build_object('name', b.name, 'imageUrl', b.logo_url)
FROM sections s
JOIN categories c ON c.name = s.category
JOIN LATERAL (
  SELECT br.id, br.name, br.logo_url
  FROM brands br
  WHERE br.category_id = c.id
    AND br.active = true
  ORDER BY br.name
  LIMIT 12
) b ON true
WHERE s.data_source = 'STATIC'
  AND s.type = 'BRAND'
  AND NOT EXISTS (SELECT 1 FROM section_items si WHERE si.section_id = s.id);

-- Verify:
-- SELECT s.title, COUNT(si.id) AS item_count
-- FROM sections s LEFT JOIN section_items si ON si.section_id = s.id
-- GROUP BY s.id, s.title ORDER BY item_count;
