# Sections Seeding Guide

Step-by-step guide to populate your sections system with real data.

---

## Step 1: Run the V22 migration

This creates all sections for all categories (empty of items for now).

```bash
./mvnw spring-boot:run
# or if using Flyway, it runs automatically on startup
```

Verify:
```sql
SELECT category, COUNT(*) FROM sections GROUP BY category;
```

You should see ~30 sections across 12+ categories.

---

## Step 2: Inspect your product table

Run the queries in `docs/INSPECT_PRODUCTS_TABLE.sql` to:
1. Find the product table name (e.g., `products`, `standard_product`)
2. List column names (e.g., `id`, `name`, `min_price_paise`, `in_stock`, `step`)
3. Check how many LIVE, in-stock products you have
4. Verify sample products

```bash
# Using your database client (psql, mysql, etc.)
psql your_database < docs/INSPECT_PRODUCTS_TABLE.sql

# OR copy-paste queries into your IDE / DBeaver
```

---

## Step 3: Populate section_items

Once you know your product table structure, use the safe insert queries from `INSPECT_PRODUCTS_TABLE.sql`:

```sql
-- Example: Insert 6 products into "Trending in Home & Living"
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
```

Repeat for other sections, adjusting:
- `section title` (match a section from V22)
- `LIMIT` (number of items per section, typically 4–6)
- `ORDER BY` clause (to control which products appear first)

---

## Step 4: Verify and adjust

Check items per section:
```sql
SELECT
  s.title,
  s.category,
  COUNT(si.id) as item_count
FROM sections s
LEFT JOIN section_items si ON s.id = si.section_id
GROUP BY s.id, s.title, s.category
ORDER BY s.position;
```

---

## Manual approach (if no products yet)

If your `products` table is empty, seed with mock items using hardcoded UUIDs:

```sql
-- Insert mock items
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
VALUES
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, '550e8400-e29b-41d4-a716-446655440000', 0, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, '550e8400-e29b-41d4-a716-446655440001', 1, NULL::jsonb),
  (gen_random_uuid(), (SELECT id FROM sections WHERE title = 'Trending in Home & Living' LIMIT 1), 'PRODUCT'::text, '550e8400-e29b-41d4-a716-446655440002', 2, NULL::jsonb);
```

The frontend will gracefully handle missing products (they won't render an image, but the section structure remains).

---

## Bulk populate all sections

Once you understand the pattern, bulk-insert across all sections:

```sql
-- For each section, grab the first N products
DO $$
DECLARE
  v_section RECORD;
  v_limit INT := 6;
BEGIN
  FOR v_section IN SELECT id, title FROM sections WHERE active = true ORDER BY position LOOP
    INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
    SELECT
      gen_random_uuid(),
      v_section.id,
      'PRODUCT'::text,
      p.id::text,
      ROW_NUMBER() OVER (ORDER BY p.id) - 1,
      NULL::jsonb
    FROM products p
    WHERE p.step = 'LIVE' AND p.in_stock = true
    LIMIT v_limit;
  END LOOP;
END $$;
```

---

## Troubleshooting

| Error | Solution |
|-------|----------|
| `column "category_name" does not exist` | Use correct column names. Run `INSPECT_PRODUCTS_TABLE.sql` to discover them. |
| `section not found` | Verify section title matches exactly. Use `SELECT title FROM sections;` to list all. |
| No products inserted | Check that `step = 'LIVE'` and `in_stock = true`. Adjust WHERE clause if needed. |
| Sections are empty on frontend | Run the inspection & insert queries above. Check `SELECT COUNT(*) FROM section_items;` |

---

## Step 5: Add banner items to sections

Banners are now part of the sections system. Insert them like any other item:

```sql
-- Insert a hero banner into the "Hero Banner" section
INSERT INTO section_items (id, section_id, item_type, item_ref_id, position, metadata)
VALUES
  (
    gen_random_uuid(),
    (SELECT id FROM sections WHERE title = 'Hero Banner' AND category = 'Mobiles & Tablets' LIMIT 1),
    'BANNER'::text,
    gen_random_uuid()::text,
    0,
    jsonb_build_object(
      'campaignId', 'diwali_2026_hero',
      'imageUrl', 'https://cdn.example.com/banners/diwali-hero.webp',
      'altText', 'Diwali Sale: Up to 70% off on phones',
      'tapAction', jsonb_build_object(
        'kind', 'DEEPLINK',
        'value', 'app://category/mobiles/offers'
      ),
      'impressionPixel', 'https://analytics.example.com/i?c=diwali_hero',
      'clickPixel', 'https://analytics.example.com/c?c=diwali_hero'
    )
  );
```

**Banner metadata fields:**
- `imageUrl` — banner image URL (required)
- `altText` — fallback text if image fails to load
- `tapAction` — what happens on tap (DEEPLINK, CATEGORY, URL, SEARCH)
- `impressionPixel` — analytics pixel fired when banner is seen
- `clickPixel` — analytics pixel fired on tap
- `campaignId` — internal ID for tracking (optional)

See **BANNER_FRONTEND_INTEGRATION.md** for how frontend renders banners.

## Next steps

1. Once seeded, test the app:
   ```bash
   ./mvnw spring-boot:run
   ```
2. **Frontend:** Update to use `GET /sections/{categoryId}` instead of `GET /banners`
3. Open app, navigate to a category → should see:
   - Hero banner at top
   - Product grids/scrolls
   - Additional banners at different positions
4. Adjust section configs (colors, layout, theme) in database to customize appearance
5. Use `POST /api/v1/sections` to add new sections/banners programmatically

**See also:** [BANNER_DEPRECATION.md](BANNER_DEPRECATION.md) for the migration path from the old Banner API
