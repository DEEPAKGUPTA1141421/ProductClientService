-- Repairs section rows whose `category` column doesn't match what V22 actually
-- intends for them. V22's INSERTs use deterministic ids (10000001-...-0000000000NN
-- per category block); this migration is the only reliable way to correct any
-- rows whose category drifted (e.g. V22 was edited after its first apply on a
-- dev DB, so the fixed category values were never re-applied — Flyway does not
-- re-run an already-applied version even if the file content later changes).
--
-- Symptom this fixes: GET /api/v1/sections/For You returning sections that
-- belong to other categories (Grocery's "Fresh & Organic", Men Fashion's
-- "Men's Collection", etc.) because their `category` column was "For You"
-- instead of their real category.
--
-- Safe/idempotent: matches by exact id prefix, only updates rows that exist,
-- and never touches the two genuine "For You" sections seeded by V29
-- (ids '10000000-f0f0-...'), which use a different id block entirely.

UPDATE sections SET category = 'Home & Living'        WHERE id::text LIKE '10000001-%';
UPDATE sections SET category = 'Kids & Toys'           WHERE id::text LIKE '10000002-%';
UPDATE sections SET category = 'Mobiles & Tablets'     WHERE id::text LIKE '10000003-%';
UPDATE sections SET category = 'Consumer Electronics'  WHERE id::text LIKE '10000004-%';
UPDATE sections SET category = 'Appliances'            WHERE id::text LIKE '10000005-%';
UPDATE sections SET category = 'Beauty & Personal Care' WHERE id::text LIKE '10000006-%';
UPDATE sections SET category = 'Sports & Fitness'      WHERE id::text LIKE '10000007-%';
UPDATE sections SET category = 'Men Fashion'           WHERE id::text LIKE '10000008-%';
UPDATE sections SET category = 'Women Fashion'         WHERE id::text LIKE '10000009-%';
UPDATE sections SET category = 'Grocery'               WHERE id::text LIKE '10000010-%';
UPDATE sections SET category = 'Books'                 WHERE id::text LIKE '10000011-%';
UPDATE sections SET category = 'Health & Wellness'     WHERE id::text LIKE '10000012-%';

-- V30's own dynamic (CATEGORY_TOP/SEARCH_QUERY) sections, in case they drifted too.
UPDATE sections SET category = 'Men Fashion'           WHERE id::text LIKE '20000001-%';
UPDATE sections SET category = 'Women Fashion'         WHERE id::text LIKE '20000002-%';
UPDATE sections SET category = 'Mobiles & Tablets'     WHERE id::text LIKE '20000003-%';
UPDATE sections SET category = 'Grocery'               WHERE id::text LIKE '20000004-%';

-- Verify — should show one row per real category, and "For You" should list
-- only 'Recommended for You' and 'Trending Now':
-- SELECT category, string_agg(title, ', ' ORDER BY position) FROM sections GROUP BY category ORDER BY category;
