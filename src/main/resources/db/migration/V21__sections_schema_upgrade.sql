-- Fix ordinal enums to STRING (safe data migration)
ALTER TABLE sections
  ALTER COLUMN type TYPE varchar(32)
  USING CASE type::int
    WHEN 0 THEN 'PRODUCT_GRID'
    WHEN 1 THEN 'PRODUCT_SCROLL'
    WHEN 2 THEN 'PRODUCT_HIGHLIGHT'
    WHEN 3 THEN 'BANNER'
    WHEN 4 THEN 'SPONSORED'
    WHEN 5 THEN 'BRAND'
    WHEN 6 THEN 'CATEGORY'
  END;

ALTER TABLE section_items
  ALTER COLUMN item_type TYPE varchar(32)
  USING CASE item_type::int
    WHEN 0 THEN 'PRODUCT'
    WHEN 1 THEN 'BANNER'
    WHEN 2 THEN 'MARKETING_PAGE'
    WHEN 3 THEN 'SPONSORED_PRODUCT'
    WHEN 4 THEN 'SPONSORED_BRAND'
    WHEN 5 THEN 'BRAND'
    WHEN 6 THEN 'CATEGORY'
  END;

-- Add new columns
ALTER TABLE sections
  ADD COLUMN IF NOT EXISTS widget_key   varchar(64)  NOT NULL DEFAULT 'product_grid_v1',
  ADD COLUMN IF NOT EXISTS data_source  varchar(32)  NOT NULL DEFAULT 'STATIC',
  ADD COLUMN IF NOT EXISTS data_params  jsonb,
  ADD COLUMN IF NOT EXISTS starts_at    timestamptz,
  ADD COLUMN IF NOT EXISTS ends_at      timestamptz,
  ADD COLUMN IF NOT EXISTS audience     jsonb,
  ADD COLUMN IF NOT EXISTS version      int NOT NULL DEFAULT 1;

ALTER TABLE section_items
  ADD COLUMN IF NOT EXISTS position int NOT NULL DEFAULT 0;

-- Indexes for query optimization
CREATE INDEX IF NOT EXISTS idx_sections_cat_active_pos
  ON sections(category, active, position) WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_section_items_section_pos
  ON section_items(section_id, position);
