-- V36 already seeded 4 PRODUCT section_items for these two "For You" sections,
-- but their data_source was left as RECO_FOR_YOU / RECO_TRENDING — those
-- values make SectionHydrator query Elasticsearch instead of reading
-- section_items, so the seeded rows were never actually served. Switch them
-- to STATIC (the same data_source hydrateStaticItems reads for every other
-- hand-curated section — see V34) so the API returns the seeded items
-- directly from Postgres, no ES/Kafka involved.

UPDATE sections
SET data_source = 'STATIC'
WHERE id IN (
  '10000000-f0f0-0000-0000-000000000001',  -- Recommended for You
  '10000000-f0f0-0000-0000-000000000002'   -- Trending Now
);
