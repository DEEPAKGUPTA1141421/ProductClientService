-- Seeds 4 PRODUCT section_items each for two specific sections.
-- Picks the 4 most-recently-created LIVE, active products (not scoped to
-- a category) and assigns them positions 0-3 per section.

INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
SELECT
  gen_random_uuid(),
  s.section_id,
  'PRODUCT',
  p.id::text,
  (ROW_NUMBER() OVER (PARTITION BY s.section_id ORDER BY p.created_at DESC) - 1)::int,
  NULL
FROM (VALUES
  ('10000000-f0f0-0000-0000-000000000001'::uuid),
  ('10000000-f0f0-0000-0000-000000000002'::uuid)
) AS s(section_id)
JOIN LATERAL (
  SELECT pr.id, pr.created_at
  FROM products pr
  WHERE pr.step = 5          -- LIVE
    AND pr.is_active = true
  ORDER BY pr.created_at DESC
  LIMIT 4
) p ON true;
