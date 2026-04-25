# Backend-Driven Sections — Architecture Plan

Target: Flipkart/Amazon-style home, category, and landing pages rendered
entirely from backend config. Frontend ships a fixed set of primitive
widgets; new layouts are added by editing JSONB, not by releasing an app.

---

## 1. Schema verdict

### Verdict: **~80% there. Keep the two tables. Add 5 columns and standardise the JSONB.**

The `sections` + `section_items` split is correct. The mistakes you will
hit at scale are all about missing fields, not missing tables:

1. **`type` is too coarse.** `PRODUCT_GRID` is a *data contract* (items are
   products) but the same data can render as a 3-col grid, a 2-col
   highlight, a horizontal scroll, or a video-card rail. Split `type` into
   **`dataKind`** (what the items are) and **`widgetKey`** (how to render).
2. **No `dataSource`.** "Suggested For You" must call the reco engine
   per-user, not read `section_items`. Today the schema can't express that.
3. **No targeting / scheduling.** Sale-day banners need start/end time,
   audience filters (city, tier, logged-in-only).
4. **No theming layer.** Background colour, font weight, padding belong in
   a structured `theme` object, not scattered keys in `config`.
5. **No versioning.** CMS edits overwrite live traffic with no rollback.

### Recommended additions to `sections`

```sql
ALTER TABLE sections
  ADD COLUMN widget_key    varchar(64) NOT NULL DEFAULT 'product_grid_v1',
  ADD COLUMN data_source   varchar(32) NOT NULL DEFAULT 'STATIC',
      -- STATIC | RECO_FOR_YOU | RECO_TRENDING | CATEGORY_TOP | SEARCH_QUERY
  ADD COLUMN data_params   jsonb,                  -- e.g. {"recoContext":"HOME","k":20}
  ADD COLUMN starts_at     timestamptz,
  ADD COLUMN ends_at       timestamptz,
  ADD COLUMN audience      jsonb,                  -- {"cities":["BLR"],"authed":true}
  ADD COLUMN version       int  NOT NULL DEFAULT 1;

CREATE INDEX idx_sections_cat_active_pos
  ON sections(category, active, position)
  WHERE active = true;
```

### Java changes (minimum)

```java
// Section.java
private String widgetKey;                     // "product_grid_v1", "video_rail_v1", …
@Enumerated(EnumType.STRING)
private DataSource dataSource;                // STATIC, RECO_FOR_YOU, …
@JdbcTypeCode(SqlTypes.JSON) private JsonNode dataParams;
private OffsetDateTime startsAt;
private OffsetDateTime endsAt;
@JdbcTypeCode(SqlTypes.JSON) private JsonNode audience;
private int version;

public enum DataSource { STATIC, RECO_FOR_YOU, RECO_TRENDING, CATEGORY_TOP, SEARCH_QUERY }
```

**Also:** switch `@Enumerated(ORDINAL)` → `@Enumerated(STRING)` on both
`SectionType` and `ItemType`. Ordinal enums silently corrupt data the
moment someone reorders the enum.

### `section_items` is fine. One addition:

```sql
ALTER TABLE section_items ADD COLUMN position int NOT NULL DEFAULT 0;
CREATE INDEX idx_section_items_section_pos ON section_items(section_id, position);
```

You cannot rely on insertion order forever.

---

## 2. Widget architecture (frontend)

### Core principle
> **The frontend has ~8 primitive widgets. The backend picks one by
> `widgetKey` and hands it a `config` + `items` payload.**

```
┌─────────────────────────────────────────────────────────┐
│                  SectionRenderer (router)               │
│   reads widgetKey → looks up in WIDGET_REGISTRY         │
└───────────────────────────┬─────────────────────────────┘
                            │
     ┌────────┬────────┬────┴────┬────────┬────────┐
     ▼        ▼        ▼         ▼        ▼        ▼
  Banner   Grid     Rail      Highlight Video    Brand
  Widget   Widget   Widget    Widget    Widget   Widget
  (full)   (N×M)    (h-scroll)(2-up big)(autoplay)(logo row)
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
         ProductCard                 BrandCard / CategoryCard
         (variants: standard | compact | highlight | spec)
```

### WIDGET_REGISTRY (the only file you edit to add a layout)

```dart
// widget_registry.dart
final Map<String, SectionBuilder> WIDGET_REGISTRY = {
  'banner_hero_v1'      : (s) => BannerWidget(config: s.config, items: s.items),
  'product_grid_v1'     : (s) => GridWidget(config: s.config, items: s.items),
  'product_scroll_v1'   : (s) => RailWidget(config: s.config, items: s.items),
  'product_highlight_v1': (s) => HighlightWidget(config: s.config, items: s.items),
  'video_rail_v1'       : (s) => VideoRailWidget(config: s.config, items: s.items),
  'brand_spotlight_v1'  : (s) => BrandRailWidget(config: s.config, items: s.items),
  'brand_feature_v1'    : (s) => BrandFeatureWidget(config: s.config, items: s.items),
  'category_tiles_v1'   : (s) => CategoryTilesWidget(config: s.config, items: s.items),
};
```

Rule: **never branch UI on `dataKind`**. Every widget receives a
normalised `items[]` from the backend — it should not care whether items
came from `section_items` or from the reco engine.

### How many base widgets: **6 layout widgets + 3 card variants.**

| # | Widget | Renders |
|---|---|---|
| 1 | `BannerWidget` | 1 full-width image/GIF/video, optional overlay CTA |
| 2 | `GridWidget` | N×M grid of cards; columns/rows from config |
| 3 | `RailWidget` | Horizontal scroll of cards; item width from config |
| 4 | `HighlightWidget` | 2-up large cards with heavier typography |
| 5 | `VideoRailWidget` | Horizontal scroll of muted autoplay video cards |
| 6 | `BrandRailWidget` | Logo chips or landscape brand cards |

Cards: `ProductCard` (variants: `standard`, `compact`, `highlight`,
`spec`), `BrandCard`, `CategoryCard`. Card variant is a **config key**,
not a separate widget.

---

## 3. Config JSONB schemas (all 8 section types)

All configs share a `theme` sub-object — no ad-hoc colour keys scattered
around.

```jsonc
// shared theme
"theme": {
  "bg": "#0F0F23",            // hex or gradient token
  "fg": "#FFFFFF",
  "accent": "#FF6B35",
  "padding": {"x": 16, "y": 12},
  "fontWeight": "bold",       // normal | medium | bold
  "cornerRadius": 12
}
```

### 3.1 `banner_hero_v1`
```jsonc
{
  "height": 180,              // dp
  "aspectRatio": "16:7",
  "autoplay": true,           // for GIF/video
  "loopMs": 4000,
  "tapAction": {              // drives deeplink routing
    "kind": "DEEPLINK",       // DEEPLINK | CATEGORY | PRODUCT | URL | SEARCH
    "value": "app://category/electronics"
  },
  "theme": { … }
}
```
`items[0].metadata` carries `imageUrl`, `videoUrl`, `altText`, `campaignId`.

### 3.2 `product_grid_v1` — "Suggested For You"
```jsonc
{
  "columns": 3,
  "rows": 2,
  "cardVariant": "standard",
  "showDiscount": true,
  "showRating": false,
  "showBadge": true,
  "imageAspectRatio": "1:1",
  "maxItems": 12,
  "theme": { … }
}
```

### 3.3 `product_scroll_v1` — horizontal rail
```jsonc
{
  "itemWidth": 140,
  "cardVariant": "compact",
  "showDiscount": true,
  "showRating": true,
  "peekNext": true,            // show 10% of next card to signal scroll
  "snap": "start",
  "maxItems": 20,
  "theme": { … }
}
```

### 3.4 `product_highlight_v1` — "Top Selection"
```jsonc
{
  "columns": 2,
  "rows": 2,
  "cardVariant": "highlight",
  "imageAspectRatio": "4:5",
  "showPriceStrikethrough": true,
  "ctaText": "Shop now",
  "theme": { "fontWeight": "bold", "padding": {"x": 16, "y": 20} }
}
```

### 3.5 `video_rail_v1` — "Fashion videos"
```jsonc
{
  "itemWidth": 220,
  "aspectRatio": "9:16",
  "autoplay": true,
  "muted": true,
  "preloadCount": 2,           // don't preload the whole rail on 3G
  "overlay": {
    "show": true,
    "position": "bottom",
    "fields": ["title", "price"]
  },
  "theme": { … }
}
```
`items[i].metadata` carries `videoUrl`, `posterUrl`, `productId`.

### 3.6 `brand_spotlight_v1` — logo row
```jsonc
{
  "itemWidth": 80,
  "shape": "circle",           // circle | square | rounded
  "showName": true,
  "maxItems": 15,
  "theme": { … }
}
```

### 3.7 `brand_feature_v1` — landscape brand cards
```jsonc
{
  "itemWidth": 280,
  "aspectRatio": "16:9",
  "showTagline": true,
  "ctaText": "Visit store",
  "theme": { … }
}
```

### 3.8 `category_tiles_v1`
```jsonc
{
  "columns": 4,
  "shape": "rounded",
  "iconSize": 56,
  "showName": true,
  "theme": { … }
}
```

---

## 4. Hybrid model — what is config-driven vs hardcoded

| Widget | Config-driven? | Complexity | Reason |
|---|---|---|---|
| `banner_hero_v1` | ✅ full | Low | Image/video + tap action; pure metadata |
| `product_grid_v1` | ✅ full | Low | Cols/rows/variant is all it is |
| `product_scroll_v1` | ✅ full | Low | Same cards, horizontal container |
| `product_highlight_v1` | ✅ full | Low | Variant of grid + heavier typography |
| `brand_spotlight_v1` | ✅ full | Low | Pure list of logos |
| `brand_feature_v1` | ✅ full | Medium | Tagline + CTA, still config |
| `category_tiles_v1` | ✅ full | Low | Icon grid |
| `video_rail_v1` | ⚠️ partial | High | Autoplay, preload, battery, scroll pause — write native Flutter logic; config toggles *behaviour*, not rendering primitives |
| Flash-sale countdown, Scratch-card, Games, Live stream | ❌ hardcoded | Very high | Stateful, time-based, real-time — too brittle to describe in JSON |

**Rule of thumb**: if the widget has *stateful animation* or *real-time
data*, hardcode it and give it a `widgetKey`. If it's a list+card layout,
drive it from config.

### Section-level theme vs item-level metadata
- **`section.config.theme`** = background, default fg, typography,
  padding. Applies to the whole rail.
- **`section_items[i].metadata`** = image/video URL, badge text, CTA,
  price override, campaign ID. Per-item overrides.

An item never defines its own background colour. A section never defines
a per-item image. Keep the boundary strict or your CMS UI becomes hell.

---

## 5. API design

### Response — `GET /api/v1/sections/{categoryId}?userId=…`

Embed items. Shipping a list of section shells + N follow-up calls
murders TTFB on mobile. Embed, but **cap items per section in the
response** and expose a pagination endpoint for infinite scrolls.

```json
{
  "success": true,
  "data": {
    "categoryId": "uuid",
    "renderedAt": "2026-04-25T09:01:00Z",
    "sections": [
      {
        "id": "uuid",
        "title": "Hero",
        "widgetKey": "banner_hero_v1",
        "dataKind": "BANNER",
        "position": 1,
        "config": { "height": 180, "aspectRatio": "16:7", "theme": {…} },
        "items": [
          { "itemType": "BANNER", "itemRefId": "camp_123",
            "metadata": {"imageUrl":"…","tapAction":{…}} }
        ]
      },
      {
        "id": "uuid",
        "title": "Suggested for you",
        "widgetKey": "product_grid_v1",
        "dataKind": "PRODUCT",
        "dataSource": "RECO_FOR_YOU",
        "position": 2,
        "config": { "columns": 3, "rows": 2, "cardVariant": "standard", "theme": {…} },
        "items": [
          { "itemType": "PRODUCT", "itemRefId": "prod_uuid",
            "product": { "title":"…","pricePaise":89900,"discountPct":20,"thumbnailUrl":"…","avgRating":4.1 },
            "metadata": { "reason": "Trending in your city" } }
        ],
        "pagination": { "nextCursor": "opaque", "hasMore": true }
      }
    ]
  }
}
```

### How items get resolved (backend orchestration)

```
SectionService.getPage(categoryId, userId):
  rows = sectionsRepo.findActiveByCategory(categoryId, now, audienceFilter(userId))
  return rows.parallelStream().map(s -> switch (s.dataSource) {
    case STATIC         -> hydrateStatic(s)            // section_items + product _mget
    case RECO_FOR_YOU   -> recoOrchestrator.forYou(userId, k, HOME)   // Phase 3
    case RECO_TRENDING  -> trendingService.top(s.dataParams)
    case CATEGORY_TOP   -> catalogService.topInCategory(s.dataParams)
    case SEARCH_QUERY   -> searchService.run(s.dataParams)
  }).toList();
```

Key points:
- Run section resolution **in parallel**. Each section is independent.
- Cap the page-level timeout (e.g. 400 ms). Slow sections drop out with
  empty items — page still renders.
- Reco sections carry `recoId` in their response so clicks can be
  attributed (see `RECO_SIMILARITY_API.md`).

### Pagination for rails
`GET /api/v1/sections/{sectionId}/items?cursor=…&limit=20`. Only
`STATIC` sections paginate the stored list; reco sections re-call the
orchestrator with a larger `k`.

### Personalisation
Personalised sections are driven by `dataSource=RECO_FOR_YOU` — the
backend fans out to your Phase 3 orchestrator. The frontend does nothing
special. If the user is a guest, the orchestrator's cold-start path
kicks in; no frontend branching needed.

---

## 6. Banner / CMS strategy

Flipkart-style banners are just images produced in Canva/Figma and
uploaded. Don't overbuild this.

### Storage
- Images/GIFs/MP4s in S3 (or Cloudflare R2) behind a CDN.
- Filenames include `campaignId` and `variant` so analytics can join.

### Record shape — `section_items.metadata` for a banner
```json
{
  "campaignId": "diwali_2026_hero",
  "variant": "A",
  "imageUrl": "https://cdn.example.com/campaigns/diwali_2026/hero_v1.webp",
  "imageUrlDark": "https://cdn.example.com/…/hero_v1_dark.webp",
  "videoUrl": null,
  "posterUrl": null,
  "altText": "Diwali offers up to 70% off",
  "tapAction": { "kind": "CATEGORY", "value": "electronics_offers" },
  "impressionPixel": "https://pixel.internal/i?c=diwali_2026_hero",
  "clickPixel":      "https://pixel.internal/c?c=diwali_2026_hero"
}
```

### CMS phase
Until you have a CMS team, a simple admin form that writes straight to
`section_items.metadata` is enough. The schema above is the contract;
replace the UI later without touching the API.

---

## 7. Phased roadmap

### Phase 1 — MVP (2–3 weeks, 1 backend + 1 frontend)
- [ ] Migration: add `widget_key`, `data_source`, `data_params`,
      `starts_at`, `ends_at`, `audience`, `version`; switch enums to STRING.
- [ ] Backfill: existing rows get `widget_key` derived from old `type`.
- [ ] `SectionService` resolves STATIC sections + RECO_FOR_YOU only.
- [ ] Ship **4 widgets**: `banner_hero_v1`, `product_grid_v1`,
      `product_scroll_v1`, `category_tiles_v1`.
- [ ] One card variant: `standard`.
- [ ] Hand-authored JSON via a Postman collection. No admin UI.
- **Exit criteria**: Home page of one category renders end-to-end from
  the DB; toggling `active` hides a rail without an app release.

### Phase 2 — CMS + breadth (4–6 weeks)
- [ ] Add `product_highlight_v1`, `brand_spotlight_v1`,
      `brand_feature_v1`, `video_rail_v1`.
- [ ] Card variants: `compact`, `highlight`, `spec`.
- [ ] Minimal admin UI (React): CRUD sections, drag-to-reorder,
      preview-in-app via a staging base URL.
- [ ] Campaign scheduling (`starts_at`/`ends_at`) enforced in the query.
- [ ] Image/video upload → S3 → CDN pipeline.
- [ ] Parallel section resolution with per-section 400 ms budget.
- **Exit criteria**: marketing can launch a Diwali page without a
  backend/frontend PR.

### Phase 3 — Personalisation + experimentation (6–8 weeks)
- [ ] A/B: two `version` rows per section, hashed-user assignment
      (reuse `ExperimentRouter` from Phase 3 of reco).
- [ ] Impression + click pixels wired to the Phase 1 interaction topic —
      section CTR becomes a first-class metric.
- [ ] Audience targeting (`audience.cities`, `audience.authed`,
      `audience.tier`) honoured in query.
- [ ] Per-user section ordering (the whole *page* becomes personalised,
      not just the items in a rail).
- [ ] Redis cache of full page response, keyed by
      `(categoryId, userSegment, abBucket)`, TTL 60 s.
- **Exit criteria**: you can answer "does layout B lift CTR vs A?"
  without a data analyst.

---

## TL;DR

- Keep the 2 tables. Add 5 columns to `sections`, 1 to `section_items`.
- Split `type` into `widgetKey` (render) + `dataKind` (payload) + `dataSource` (where items come from).
- Ship 6 layout widgets + 3 card variants. Everything else is config.
- Hardcode only stateful widgets (video, countdowns, games).
- Embed items in the page response; reco sections fan out to Phase 3.
- Don't build a CMS until Phase 2 — a Postman collection is enough to prove the model.
