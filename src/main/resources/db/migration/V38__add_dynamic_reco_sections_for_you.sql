-- Adds two genuinely dynamic sections to the "For You" feed, using the
-- RECO_FOR_YOU / RECO_TRENDING data sources (ES-backed, via SectionHydrator
-- -> RecoOrchestrator / ColdStartResolver + FeatureHydrator). Unlike the two
-- sections fixed in V37 (which are hand-curated and now STATIC), these are
-- meant to be populated purely from the reco pipeline — no section_items
-- rows needed or read for them.
--
-- NOTE: both data sources query the Elasticsearch "products-v1" index
-- (step=LIVE, in_stock=true). If that index is empty/unpopulated, these
-- sections will return items: [] until it's backfilled — that's a separate,
-- already-flagged indexing/data issue, not a bug in these sections.

INSERT INTO sections (id, title, type, widget_key, data_source, config, data_params, position, active, category, starts_at, version)
VALUES
  (
    '10000000-f0f0-0000-0000-000000000003',
    'Picked For You',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_FOR_YOU',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FFFFFF"}}'::jsonb,
    '{"k":12}'::jsonb,
    3,
    true,
    'For You',
    NOW(),
    1
  ),
  (
    '10000000-f0f0-0000-0000-000000000004',
    'Trending Right Now',
    'PRODUCT_GRID',
    'product_grid_v1',
    'RECO_TRENDING',
    '{"columns":3,"rows":2,"cardVariant":"standard","showDiscount":true,"showRating":true,"theme":{"bg":"#FAFAFA"}}'::jsonb,
    '{"k":12}'::jsonb,
    4,
    true,
    'For You',
    NOW(),
    1
  )
ON CONFLICT (id) DO NOTHING;
