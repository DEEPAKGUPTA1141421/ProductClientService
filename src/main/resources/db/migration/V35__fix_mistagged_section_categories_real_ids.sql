-- Real fix for the "For You" section-leakage bug, superseding V31 (which
-- matched by the deterministic ids V22's *current* file content uses —
-- '10000001-...' etc. — but this DB's rows were seeded from an earlier
-- version of V22 that used gen_random_uuid() ids instead, so V31 matched
-- nothing). Identified below via a live dump of `sections` (title +
-- widget_key + data_source + position + config), matched 1:1 against V22.
--
-- Left untouched (genuinely belong under 'For You'):
--   10000000-f0f0-0000-0000-000000000001  Recommended for You  (V29)
--   10000000-f0f0-0000-0000-000000000002  Trending Now         (V29)
--   bfdad276-443c-44a6-b7a2-e791deeb5382  Featured Looks       (hand-authored, not in V22)

UPDATE sections SET category = 'Home & Living' WHERE id IN (
  'f56d2bae-2da5-4995-9fe3-5e109d8ff697',  -- Hero Banner
  '861a9657-67de-4e2a-b180-fea689ad70fe',  -- Trending in Home & Living
  '70886cf3-c7e4-4fc2-b95a-4e3f41054258',  -- Premium Picks
  '5450227d-1ef2-4cd5-a793-21040ef33b5d',  -- Top Brands
  'f229bb2e-4abb-450d-9996-02bd6cae90a6'   -- New Arrivals (itemWidth 160, snap start)
);

UPDATE sections SET category = 'Mobiles & Tablets' WHERE id IN (
  'eb22121e-0a50-405f-8bb9-818cf81e733b',  -- Latest Phones
  '8bb1ca6d-954f-41ac-8b80-ad19c4799bf8',  -- Best Sellers
  '155e7595-e578-4508-b023-d7f0e5806e66',  -- Premium Flagships
  'daa0407c-f3ba-4845-af2a-472e44dbe77b'   -- Popular Brands
);

UPDATE sections SET category = 'Consumer Electronics' WHERE id IN (
  '1d69270f-cb84-4659-b136-942179328ca2',  -- Great Tech Deals
  'dc9cd740-3a51-4d14-a6fe-87c9b99470bf',  -- Featured Electronics
  '70aa9524-d5be-4b87-b0ce-7213c98c8135'   -- Bestselling Gadgets
);

UPDATE sections SET category = 'Beauty & Personal Care' WHERE id IN (
  '4f186517-b522-4323-82c6-97b6ff278f7f',  -- Beauty Essentials
  '8875ffb6-965b-45f5-a7b2-38a38b4cc53f',  -- Top Rated Products
  '556b6101-6ffb-4902-9455-4438b237a885'   -- Premium Skincare
);

UPDATE sections SET category = 'Sports & Fitness' WHERE id IN (
  '62867965-e339-4ba1-be48-dd0561c32b41',  -- Fitness Gear
  '417a4067-88ef-4d38-8e94-4017d54b1448',  -- Trending Equipment
  'e463f01b-d85a-4796-8556-a83714971ac5'   -- New Arrivals (itemWidth 170)
);

UPDATE sections SET category = 'Men Fashion' WHERE id IN (
  '51fedf4a-7a9b-4b6a-b261-ce3a47c42ca9',  -- Men's Collection
  'd168af71-a072-481d-a0c4-1e86b9735dc2',  -- Top Trending
  'c47b1da1-ba94-4a8b-913b-fae65b9d4402'   -- Premium Selection
);

UPDATE sections SET category = 'Women Fashion' WHERE id IN (
  'beea6ad7-c277-40ba-97f4-87ad0b4b5307',  -- Women's Fashion
  '97f64e22-cbd0-4c12-bdac-846c680c8f71',  -- Trending Now
  '6fdc69d8-def1-434d-9ecf-6868ccbbd403'   -- Exclusive Styles
);

UPDATE sections SET category = 'Grocery' WHERE id IN (
  '0ead0450-5df4-47cc-b3c1-d784d5632306',  -- Fresh & Organic
  '48192fe8-ca96-478f-ab73-9ce3c717bc06'   -- Popular Items
);

-- Verify — "For You" should now show only 3 rows:
-- SELECT title, data_source, position FROM sections WHERE category = 'For You' ORDER BY position;
