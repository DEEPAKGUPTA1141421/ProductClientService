# Sections + Banners System Summary

**The unified architecture for homepage & category pages.**

---

## What Changed?

| Element | Before | After |
|---------|--------|-------|
| **Banner storage** | Separate `Banner` table | `BANNER` items in `section_items` |
| **API endpoints** | `GET /banners`, `POST /banners`, `DELETE /banners` | `GET /sections/{categoryId}` (includes banners) |
| **Flexibility** | 1 banner per category | Multiple banners at any position |
| **Scheduling** | Manual timestamps | Native `starts_at`/`ends_at` |
| **Analytics** | Custom tracking | Metadata pixels (`impressionPixel`, `clickPixel`) |
| **Frontend calls** | 2 calls (banners + products) | 1 call (sections with both) |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  GET /api/v1/sections/{categoryId}           │
│  (Replaces: GET /banners + GET /products)                    │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        ▼                                       ▼
   BANNER Sections                        PRODUCT Sections
   ├─ Position 1: Hero Banner            ├─ Position 2: Grid
   ├─ Position 4: Mid-Page Banner       ├─ Position 3: Scroll
   └─ Position 7: Celebrity Picks       └─ Position 5: Highlight

   items: [BANNER itemType]             items: [PRODUCT itemType]
   metadata: {imageUrl,                 data: {title, price, rating,
              tapAction,                       thumbnail, ...}
              pixels}
```

---

## Data Flow

### Backend (Java)

1. **V22 migration** creates 30+ sections across all categories (empty of items)
2. **Section items** can be:
   - `BANNER` type → metadata contains image URL, deeplinks, pixels
   - `PRODUCT` type → full product details hydrated
3. **SectionService.getPage()** returns list of `SectionResponseDto`
   - Each section has its own renderer hint (`widgetKey`)
   - Each section carries its items (banners or products)

### Frontend (Dart/Flutter)

1. **Fetch** `GET /sections/{categoryId}`
2. **Parse** response into `SectionResponseDto` list
3. **Loop** through sections in order:
   - If `isBanner()` → render `_BannerSection` widget
   - If `isProductGrid()` → render `_ProductGridSection` widget
   - etc.
4. **Banner rendering:**
   - Load image from `metadata.imageUrl`
   - Fire impression pixel on widget init
   - Fire click pixel + handle `tapAction` on tap

---

## File Structure

### Backend docs
```
docs/
├─ BANNER_DEPRECATION.md               ← Migration timeline & strategy
├─ SECTIONS_ARCHITECTURE.md            ← Original design spec
├─ SECTIONS_SEEDING_GUIDE.md           ← How to populate data
├─ INSPECT_PRODUCTS_TABLE.sql          ← Schema discovery + inserts
├─ SEED_SECTIONS_DATA.md               ← Detailed seed queries
└─ SECTIONS_BANNERS_SUMMARY.md         ← This file
```

### Frontend docs
```
lib/
├─ RECO_SIMILARITY_API.md              ← Phase 1-3 tracking & reco APIs
├─ SECTIONS_BANNERS_INTEGRATION.md     ← Complete Dart implementation guide
```

---

## Database Schema

### sections table
```
id          | uuid
title       | varchar (e.g., "Hero Banner", "Product Grid")
type        | enum (BANNER, PRODUCT_GRID, PRODUCT_SCROLL, etc.)
widget_key  | varchar (e.g., "banner_hero_v1", "product_grid_v1")
config      | jsonb (layout, colors, theme, card variant, etc.)
position    | int (order on page; lower = higher)
category    | varchar (e.g., "Mobiles & Tablets")
starts_at   | timestamptz (campaign start; null = always on)
ends_at     | timestamptz (campaign end; null = always on)
active      | boolean
version     | int (for A/B testing)
```

### section_items table
```
id          | uuid
section_id  | uuid (FK → sections)
item_type   | enum ('BANNER', 'PRODUCT', 'BRAND', 'CATEGORY')
item_ref_id | varchar (product UUID, banner ID, etc.)
position    | int (order within section)
metadata    | jsonb (image URLs, deeplinks, pixels for banners; null for products)
```

---

## Complete Example: Diwali Sale Page

**Database setup:**

```sql
-- Section 1: Hero banner
INSERT INTO sections VALUES (
  id='sec-1', title='Diwali Hero', type='BANNER', widget_key='banner_hero_v1',
  position=1, category='Mobiles & Tablets', active=true, ...
);

-- Section 2: Featured products
INSERT INTO sections VALUES (
  id='sec-2', title='Best Offers', type='PRODUCT_GRID', widget_key='product_grid_v1',
  position=2, category='Mobiles & Tablets', active=true, ...
);

-- Section 3: Celebrity picks banner
INSERT INTO sections VALUES (
  id='sec-3', title='Celebrity Picks', type='BANNER', widget_key='banner_hero_v1',
  position=4, category='Mobiles & Tablets', active=true, ...
);

-- Insert items
INSERT INTO section_items VALUES (
  section_id='sec-1', item_type='BANNER', 
  metadata='{"imageUrl":"...hero.webp", "tapAction":{"kind":"DEEPLINK","value":"..."}}'
);

INSERT INTO section_items VALUES (
  section_id='sec-2', item_type='PRODUCT', item_ref_id='product-uuid-1'
), (...), ...;

INSERT INTO section_items VALUES (
  section_id='sec-3', item_type='BANNER',
  metadata='{"imageUrl":"...celebrity.webp", ...}'
);
```

**API response:**
```json
GET /api/v1/sections/mobiles-tablets-id
→ [
    {title: "Diwali Hero", items: [{BANNER with image}]},
    {title: "Best Offers", items: [{PRODUCT}, {PRODUCT}, ...]},
    {title: "Celebrity Picks", items: [{BANNER with image}]}
  ]
```

**Frontend rendering:**
```
┌─────────────────────────────────┐
│  Diwali Hero Banner (hero.webp) │  ← BANNER section #1
├─────────────────────────────────┤
│  Best Offers Grid               │  ← PRODUCT section #2
│  ┌──────┬──────┬──────┐         │
│  │iPhone│Samsung│Oneplus       │
│  └──────┴──────┴──────┘         │
├─────────────────────────────────┤
│  Celebrity Picks Banner         │  ← BANNER section #3
│  (celebrity.webp)               │
└─────────────────────────────────┘
```

---

## Analytics Integration

Each banner has:
- **impressionPixel**: Fired when banner scrolls into view
- **clickPixel**: Fired when user taps banner

```dart
// Frontend fires pixel on render
final pixelUrl = item.impressionPixel;
http.get(Uri.parse(pixelUrl)).catchError((_) {}); // background

// Backend receives ping with campaign ID
// → tracks impressions, CTR, etc.
```

---

## Migration Checklist

### Backend
- [x] Create V22 migration (sections infrastructure)
- [x] Create V21 migration (schema updates)
- [x] Document deprecation path (BANNER_DEPRECATION.md)
- [ ] Keep Banner API working (for backward compat)
- [ ] Plan removal (v2.2, Q2 2026)

### Frontend
- [ ] Update section DTOs (SectionResponseDto, SectionItemDto)
- [ ] Implement banner widget (_BannerSection)
- [ ] Update category page to use `/sections` API
- [ ] Remove calls to old `/banners` endpoint
- [ ] Test multi-banner layouts
- [ ] Wire up impression & click pixels

### Data
- [ ] Populate BANNER section items for all categories
- [ ] Migrate existing Banner records to section_items
- [ ] Verify campaign IDs & pixels

---

## References

- **Backend deprecation plan:** [BANNER_DEPRECATION.md](BANNER_DEPRECATION.md)
- **Frontend implementation:** [SECTIONS_BANNERS_INTEGRATION.md](../Frontend/user_app/SECTIONS_BANNERS_INTEGRATION.md)
- **Data population:** [SECTIONS_SEEDING_GUIDE.md](SECTIONS_SEEDING_GUIDE.md)
- **Original architecture:** [SECTIONS_ARCHITECTURE.md](SECTIONS_ARCHITECTURE.md)
