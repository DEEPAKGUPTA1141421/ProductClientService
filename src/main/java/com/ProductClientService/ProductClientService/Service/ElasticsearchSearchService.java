package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse.SearchProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ElasticsearchSearchService
 * ───────────────────────────
 * Executes all product searches against the "products-v1" ES index.
 * Replaces the native SQL in SearchResultsRepository for the hot search path.
 *
 * Query anatomy
 * ─────────────
 *  bool
 *   ├── filter: step=LIVE, in_stock=true
 *   ├── filter: category_id, brand_ids, price range, rating, discount, attrs (nested)
 *   ├── should (boosted): multi_match on name^3, brand_name^2, description, attr values^2
 *   └── function_score: field_value_factor(ranking_score, log1p)
 *
 * Sort
 * ────
 *  rel        → _score DESC then ranking_score DESC
 *  price_asc  → min_price_paise ASC
 *  price_desc → min_price_paise DESC
 *  rating     → avg_rating DESC
 *  newest     → created_at DESC
 *  discount   → discount_percent DESC
 *
 * The response is mapped directly to SearchProductDto — no additional DB call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchService {

    private final ElasticsearchClient esClient;

    static final String INDEX = "products-v1";

    // ── Main search ───────────────────────────────────────────────────────────

    public SearchResultsResponse search(
            com.ProductClientService.ProductClientService.DTO.search.SearchRequest req,
            UUID userId) {

        try {
            SearchRequest esReq = buildEsRequest(req);
            SearchResponse<ProductSearchDocument> resp =
                    esClient.search(esReq, ProductSearchDocument.class);

            long total = resp.hits().total() != null ? resp.hits().total().value() : 0L;
            List<Hit<ProductSearchDocument>> hits = resp.hits().hits();

            List<SearchProductDto> products = hits.stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .map(this::toDto)
                    .toList();

            boolean hasMore = ((long) req.getPage() + 1) * req.getPageSize() < total;

            // Build next cursor from the last hit's sort values (used with search_after)
            String sortBy = req.getSortBy() == null ? "rel" : req.getSortBy();
            String nextCursor = null;
            if (hasMore && !hits.isEmpty()) {
                List<FieldValue> lastSort = hits.get(hits.size() - 1).sort();
                if (lastSort != null && !lastSort.isEmpty()) {
                    nextCursor = encodeCursor(sortBy, lastSort);
                }
            }

            SearchResultsResponse.ShopScope shopScope = req.getSellerId() != null
                    ? SearchResultsResponse.ShopScope.builder().sellerId(req.getSellerId()).build()
                    : null;

            return SearchResultsResponse.builder()
                    .totalCount(total)
                    .page(req.getPage())
                    .pageSize(req.getPageSize())
                    .hasMore(hasMore)
                    .products(products)
                    .shopScope(shopScope)
                    .nextCursor(nextCursor)
                    .build();

        } catch (Exception e) {
            log.error("ES search failed, returning empty result: {}", e.getMessage());
            return SearchResultsResponse.builder()
                    .totalCount(0).page(req.getPage()).pageSize(req.getPageSize())
                    .hasMore(false).products(List.of()).build();
        }
    }

    // ── ES request builder ────────────────────────────────────────────────────

    private SearchRequest buildEsRequest(
            com.ProductClientService.ProductClientService.DTO.search.SearchRequest req) {

        List<Query> filters = new ArrayList<>();

        // Always: only LIVE, in-stock products
        // Accepts both "LIVE" (re-indexed docs) and "4" (legacy ordinal docs)
        filters.add(Query.of(q -> q.terms(t -> t
                .field("step")
                .terms(tv -> tv.value(List.of(
                        FieldValue.of("LIVE"),
                        FieldValue.of("4")))))));
        filters.add(term("in_stock", "true"));

        // Category
        if (req.getCategoryId() != null) {
            filters.add(term("category_id", req.getCategoryId().toString()));
        }

        // Seller / shop scope — restricts results to one shop's products
        if (req.getSellerId() != null) {
            filters.add(term("seller_id", req.getSellerId().toString()));
        }

        // Brand (terms = OR across multiple IDs)
        if (req.getBrandIds() != null && !req.getBrandIds().isEmpty()) {
            List<FieldValue> brandValues = req.getBrandIds().stream()
                    .map(id -> FieldValue.of(id.toString()))
                    .toList();
            filters.add(Query.of(q -> q.terms(t -> t
                    .field("brand_id")
                    .terms(tv -> tv.value(brandValues)))));
        }

        // Price range (paise)
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.field("min_price_paise");
                if (req.getMinPrice() != null)
                    r.gte(JsonData.of((long)(req.getMinPrice() * 100)));
                if (req.getMaxPrice() != null)
                    r.lte(JsonData.of((long)(req.getMaxPrice() * 100)));
                return r;
            })));
        }

        // Rating
        if (req.getMinRating() != null) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("avg_rating")
                    .gte(JsonData.of(req.getMinRating())))));
        }

        // Discount
        if (req.getMinDiscountPercent() != null && req.getMinDiscountPercent() > 0) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("discount_percent")
                    .gte(JsonData.of(req.getMinDiscountPercent())))));
        }

        // Top-rated badge
        if (Boolean.TRUE.equals(req.getTopRated())) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("avg_rating").gte(JsonData.of(4.0)))));
        }

        // New arrivals (last 30 days)
        if (Boolean.TRUE.equals(req.getNewArrivals())) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("created_at")
                    .gte(JsonData.of("now-30d/d")))));
        }

        // Free delivery
        if (Boolean.TRUE.equals(req.getFreeDelivery())) {
            filters.add(term("free_delivery", "true"));
        }

        // Attribute filter (nested — prevents cross-attribute false positives)
        if (req.getAttributeName() != null && req.getAttributeValues() != null
                && !req.getAttributeValues().isEmpty()) {

            String attrName = req.getAttributeName().toLowerCase();
            List<FieldValue> attrFieldValues = req.getAttributeValues().stream()
                    .map(v -> FieldValue.of(v.toLowerCase()))
                    .toList();

            filters.add(Query.of(q -> q.nested(n -> n
                    .path("attributes")
                    .query(inner -> inner.bool(b -> b
                            .must(m -> m.term(t -> t
                                    .field("attributes.name")
                                    .value(attrName)))
                            .must(m -> m.terms(t -> t
                                    .field("attributes.value")
                                    .terms(tv -> tv.value(attrFieldValues)))))))));
        }

        // ── Keyword (should clause — boosts score but doesn't exclude) ────────
        List<Query> shouldClauses = new ArrayList<>();
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword().trim();
            if (kw.length() > 120) kw = kw.substring(0, 120);
            final String kwFinal = kw;

            // Fuzzy full-word match (handles typos, complete words)
            shouldClauses.add(Query.of(q -> q.multiMatch(m -> m
                    .query(kwFinal)
                    .fields(Arrays.asList("name^3", "brand_name^2", "description",
                            "category_name", "tags"))
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))));

            // Prefix match — handles incremental typing ("str" → "striped", "lond" → "London")
            // Uses phrase_prefix so the last token in the query is expanded as a prefix.
            shouldClauses.add(Query.of(q -> q.multiMatch(m -> m
                    .query(kwFinal)
                    .fields(Arrays.asList("name^4", "brand_name^2", "category_name"))
                    .type(TextQueryType.PhrasePrefix)
                    .maxExpansions(50))));
        }

        // ── Bool query ────────────────────────────────────────────────────────
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder()
                .filter(filters);
        if (!shouldClauses.isEmpty()) {
            boolBuilder.should(shouldClauses).minimumShouldMatch("1");
        }
        Query boolQuery = Query.of(q -> q.bool(boolBuilder.build()));

        // ── Function score (ranking_score boosts relevance sort) ──────────────
        Query scoredQuery = Query.of(q -> q.functionScore(fs -> fs
                .query(boolQuery)
                .functions(fn -> fn.fieldValueFactor(fvf -> fvf
                        .field("ranking_score")
                        .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Log1p)
                        .missing(0.0)))
                .boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Sum)));

        // ── Sort + pagination ─────────────────────────────────────────────────
        String sortBy = req.getSortBy() == null ? "rel" : req.getSortBy();
        // Route shop-scoped queries to the shard that owns this seller's docs.
        String routing = req.getSellerId() != null ? req.getSellerId().toString() : null;

        // Cursor takes priority over offset. Decode cursor → search_after values.
        // product_id is the stable tie-break appended to every sort, so cursor values always end with product_id.
        List<FieldValue> searchAfter = null;
        int from = req.getPage() * req.getPageSize();
        if (req.getCursor() != null && !req.getCursor().isBlank()) {
            searchAfter = decodeCursor(req.getCursor(), sortBy);
            if (searchAfter != null) from = 0; // search_after replaces offset
        }
        final int fromFinal = from;
        final List<FieldValue> searchAfterFinal = searchAfter;

        return switch (sortBy) {
            case "price_asc" -> SearchRequest.of(s -> {
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.field(f -> f.field("min_price_paise").order(SortOrder.Asc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
            case "price_desc" -> SearchRequest.of(s -> {
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.field(f -> f.field("min_price_paise").order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
            case "rating" -> SearchRequest.of(s -> {
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.field(f -> f.field("avg_rating").order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
            case "newest" -> SearchRequest.of(s -> {
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.field(f -> f.field("created_at").order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
            case "discount" -> SearchRequest.of(s -> {
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.field(f -> f.field("discount_percent").order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
            default -> SearchRequest.of(s -> { // "rel" — relevance + ranking_score
                s.index(INDEX).query(scoredQuery).from(fromFinal).size(req.getPageSize())
                 .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("ranking_score").order(SortOrder.Desc)))
                 .sort(so -> so.field(f -> f.field("product_id").order(SortOrder.Asc)));
                if (routing != null) s.routing(routing);
                if (searchAfterFinal != null) s.searchAfter(searchAfterFinal);
                return s;
            });
        };
    }

    // ── Document → DTO ────────────────────────────────────────────────────────

    private SearchProductDto toDto(ProductSearchDocument doc) {
        double price        = doc.getMinPricePaise() / 100.0;
        double origPrice    = doc.getOriginalPricePaise() / 100.0;
        Integer discPct     = doc.getDiscountPercent() > 0 ? doc.getDiscountPercent() : null;

        String badge = null;
        if (doc.getAvgRating() >= 4.5 && doc.getReviewCount() > 1000) badge = "Bestseller";
        else if (doc.getAvgRating() >= 4.3)                            badge = "Top Rated";
        else if (discPct != null && discPct >= 20)                     badge = discPct + "% Off";

        // TODO: compute per-seller delivery estimate once logistics data is available
        String delivText = "EXPRESS · Delivering Tomorrow";

        return SearchProductDto.builder()
                .id(doc.getProductId() != null ? UUID.fromString(doc.getProductId()) : null)
                .name(doc.getName())
                .brand(doc.getBrandName())
                .brandId(doc.getBrandId() != null ? UUID.fromString(doc.getBrandId()) : null)
                .price(price)
                .originalPrice(origPrice > price ? origPrice : null)
                .discountPercent(discPct)
                .rating(doc.getAvgRating())
                .reviewCount(doc.getReviewCount())
                .images(cleanImages(doc.getImages()))
                .hasVideo(false)
                .badge(badge)
                .deliveryText(delivText)
                .freeDelivery(doc.isFreeDelivery())
                .isSponsored(false)
                .isWishlisted(false)     // injected post-cache by SearchResultsService
                .variantId(doc.getVariantId() != null ? UUID.fromString(doc.getVariantId()) : null)
                .categoryId(doc.getCategoryId() != null ? UUID.fromString(doc.getCategoryId()) : null)
                .categoryName(doc.getCategoryName())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Images stored in ES may carry extra JSON encoding artefacts, e.g.:
    //   "\"https://...png\""   or   "[\"https://...png\""   or   "\"https://...png\"]"
    // Extract just the URL from each string element.
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\s\"\\\\\\[\\]]+");

    private static List<String> cleanImages(List<String> raw) {
        if (raw == null) return List.of();
        List<String> result = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            Matcher m = URL_PATTERN.matcher(s);
            if (m.find()) result.add(m.group());
        }
        return result;
    }

    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private String buildDeliveryText(int days, boolean free) {
        String suffix = free ? ", Free" : "";
        return switch (days) {
            case 0  -> "Today" + suffix;
            case 1  -> "Tomorrow" + suffix;
            default -> "In " + days + " Days" + (free ? ", Free" : "");
        };
    }

    // ── Cursor encode / decode ────────────────────────────────────────────────
    // Format (before base64): `<sortBy>:<typeChar><value>:<typeChar><value>:...`
    // typeChar: 'd' = double, 'l' = long, 's' = string, 'n' = null
    // The last value is always the product_id (string) tie-break.
    // UUIDs and numeric strings never contain ':', so splitting on ':' after the
    // first segment is safe for all current sort fields.

    public String encodeCursor(String sortBy, List<FieldValue> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) return null;
        try {
            StringBuilder sb = new StringBuilder(sortBy);
            for (FieldValue fv : sortValues) {
                sb.append(':');
                if (fv.isNull()) {
                    sb.append('n');
                } else {
                    switch (fv._kind()) {
                        case Double  -> sb.append('d').append(fv.doubleValue());
                        case Long    -> sb.append('l').append(fv.longValue());
                        case String  -> sb.append('s').append(fv.stringValue());
                        case Boolean -> sb.append('b').append(fv.booleanValue());
                        default      -> sb.append('n');
                    }
                }
            }
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Cursor encode failed: {}", e.getMessage());
            return null;
        }
    }

    public List<FieldValue> decodeCursor(String cursor, String sortBy) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            // First segment is sortBy; remaining are typed values
            int firstColon = raw.indexOf(':');
            if (firstColon < 0) return null;
            String embeddedSort = raw.substring(0, firstColon);
            if (!sortBy.equals(embeddedSort)) {
                log.warn("Cursor sortBy mismatch: expected={}, got={}", sortBy, embeddedSort);
                return null;
            }
            String[] parts = raw.substring(firstColon + 1).split(":", -1);
            List<FieldValue> values = new ArrayList<>(parts.length);
            for (String p : parts) {
                if (p.isEmpty() || p.equals("n")) {
                    values.add(FieldValue.NULL);
                    continue;
                }
                char type = p.charAt(0);
                String val  = p.substring(1);
                values.add(switch (type) {
                    case 'd' -> FieldValue.of(Double.parseDouble(val));
                    case 'l' -> FieldValue.of(Long.parseLong(val));
                    case 's' -> FieldValue.of(val);
                    case 'b' -> FieldValue.of(Boolean.parseBoolean(val));
                    default  -> FieldValue.NULL;
                });
            }
            return values.isEmpty() ? null : values;
        } catch (Exception e) {
            log.warn("Cursor decode failed: {}", e.getMessage());
            return null;
        }
    }
}
