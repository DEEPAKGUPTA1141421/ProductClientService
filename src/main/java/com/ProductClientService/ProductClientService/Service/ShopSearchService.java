package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.DistanceUnit;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.ProductClientService.ProductClientService.DTO.search.ShopFilterRequest;
import com.ProductClientService.ProductClientService.DTO.search.ShopSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ShopSearchService
 * ──────────────────
 * All Elasticsearch queries against the "shops-v1" index.
 *
 * Three operations:
 *  1. nearby()     — geo_distance filter + geo-distance sort (primary listing)
 *  2. search()     — multi_match text search + optional geo filter
 *  3. suggest()    — search_as_you_type prefix on display_name_suggest
 *
 * The returned {@link ShopSearchDocument} list is consumed by ShopService,
 * which enriches each entry with delivery ETA via DeliveryInventoryService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopSearchService {

    private final ElasticsearchClient esClient;

    static final String INDEX = "shops-v1";

    // ── Nearby ────────────────────────────────────────────────────────────────

    /**
     * Returns ACTIVE shops within {@code req.radiusKm} of the user,
     * sorted by the requested sort strategy.
     *
     * @return raw ES hits (not yet delivery-enriched)
     */
    public List<ShopSearchDocument> nearby(ShopFilterRequest req) {
        try {
            List<Query> filters = baseFilters(req);

            // Geo radius filter
            filters.add(geoDistanceFilter(req.getUserLat(), req.getUserLng(), req.getRadiusKm()));

            BoolQuery boolQuery = BoolQuery.of(b -> b.filter(filters));
            Query query = Query.of(q -> q.bool(boolQuery));

            SearchRequest esReq = buildSortedRequest(query, req);
            SearchResponse<ShopSearchDocument> resp = esClient.search(esReq, ShopSearchDocument.class);

            return hitsToList(resp);

        } catch (Exception e) {
            log.error("ES nearby query failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ── Text search ───────────────────────────────────────────────────────────

    /**
     * Full-text shop search. Applies optional geo filter when userLat/userLng
     * are non-zero (i.e., location is known).
     */
    public List<ShopSearchDocument> search(ShopFilterRequest req) {
        try {
            List<Query> filters = baseFilters(req);

            // Geo filter when location is known (not 0,0)
            if (req.getUserLat() != 0.0 || req.getUserLng() != 0.0) {
                filters.add(geoDistanceFilter(req.getUserLat(), req.getUserLng(), req.getRadiusKm()));
            }

            // Keyword — must clause
            String kw = req.getKeyword() == null ? "" : req.getKeyword().trim();
            List<Query> mustClauses = new ArrayList<>();
            if (!kw.isBlank()) {
                mustClauses.add(Query.of(q -> q.multiMatch(m -> m
                        .query(kw)
                        .fields(List.of(
                                "display_name^3",
                                "display_name_suggest^2",
                                "tags^2",
                                "category_name",
                                "city",
                                "legal_name"))
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO"))));
            }

            BoolQuery boolQuery = BoolQuery.of(b -> {
                b.filter(filters);
                if (!mustClauses.isEmpty()) b.must(mustClauses);
                return b;
            });

            Query query = Query.of(q -> q.bool(boolQuery));
            SearchRequest esReq = buildSortedRequest(query, req);
            SearchResponse<ShopSearchDocument> resp = esClient.search(esReq, ShopSearchDocument.class);

            return hitsToList(resp);

        } catch (Exception e) {
            log.error("ES shop search failed for keyword='{}': {}", req.getKeyword(), e.getMessage(), e);
            return List.of();
        }
    }

    // ── Autocomplete suggestions ───────────────────────────────────────────────

    /**
     * Returns up to {@code limit} shop name suggestions using
     * the search_as_you_type field {@code display_name_suggest}.
     */
    public List<String> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();

        try {
            String q = prefix.trim();
            Query suggestQuery = Query.of(root -> root.bool(b -> b
                    .filter(term("status", "ACTIVE"))
                    .must(must -> must.multiMatch(m -> m
                            .query(q)
                            .type(TextQueryType.BoolPrefix)
                            .fields(List.of(
                                    "display_name_suggest",
                                    "display_name_suggest._2gram",
                                    "display_name_suggest._3gram"))))));

            SearchRequest esReq = SearchRequest.of(s -> s
                    .index(INDEX)
                    .query(suggestQuery)
                    .size(limit)
                    .source(src -> src.filter(f -> f.includes("display_name"))));

            SearchResponse<ShopSearchDocument> resp = esClient.search(esReq, ShopSearchDocument.class);

            return resp.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null && doc.getDisplayName() != null)
                    .map(ShopSearchDocument::getDisplayName)
                    .distinct()
                    .toList();

        } catch (Exception e) {
            log.error("ES shop suggest failed for prefix='{}': {}", prefix, e.getMessage(), e);
            return List.of();
        }
    }

    // ── Single shop lookup ────────────────────────────────────────────────────

    /**
     * Fetches a single shop document from ES by shopId (keyword field).
     */
    public ShopSearchDocument getById(String shopId) {
        try {
            SearchRequest esReq = SearchRequest.of(s -> s
                    .index(INDEX)
                    .query(q -> q.term(t -> t.field("shop_id").value(shopId)))
                    .size(1));

            SearchResponse<ShopSearchDocument> resp = esClient.search(esReq, ShopSearchDocument.class);
            return resp.hits().hits().stream()
                    .map(Hit::source)
                    .filter(doc -> doc != null)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("ES getById failed for shopId={}: {}", shopId, e.getMessage(), e);
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Always-on filters: status=ACTIVE, optional categoryId, optional minRating. */
    private List<Query> baseFilters(ShopFilterRequest req) {
        List<Query> filters = new ArrayList<>();
        filters.add(term("status", "ACTIVE"));

        if (req.getCategoryId() != null) {
            filters.add(term("category_id", req.getCategoryId().toString()));
        }
        if (req.getMinRating() != null) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("avg_rating")
                    .gte(JsonData.of(req.getMinRating())))));
        }
        return filters;
    }

    private Query geoDistanceFilter(double lat, double lng, double radiusKm) {
        return Query.of(q -> q.geoDistance(gd -> gd
                .field("location")
                .location(gl -> gl.latlon(ll -> ll.lat(lat).lon(lng)))
                .distance(radiusKm + "km")
                .distanceType(GeoDistanceType.Arc)));
    }

    /** Builds the final SearchRequest with sort + pagination. */
    private SearchRequest buildSortedRequest(Query query, ShopFilterRequest req) {
        int from = req.getPage() * req.getPageSize();
        String sortBy = req.getSortBy() == null ? "distance" : req.getSortBy();

        return switch (sortBy) {
            case "rating" -> SearchRequest.of(s -> s
                    .index(INDEX).query(query).from(from).size(req.getPageSize())
                    .sort(so -> so.field(f -> f.field("avg_rating").order(SortOrder.Desc)))
                    .sort(so -> so.field(f -> f.field("ranking_score").order(SortOrder.Desc))));

            case "name" -> SearchRequest.of(s -> s
                    .index(INDEX).query(query).from(from).size(req.getPageSize())
                    .sort(so -> so.field(f -> f.field("display_name.keyword").order(SortOrder.Asc))));

            default -> // "distance" — geo-distance from user, closest first
                    SearchRequest.of(s -> s
                            .index(INDEX).query(query).from(from).size(req.getPageSize())
                            .sort(so -> so.geoDistance(gd -> gd
                                    .field("location")
                                    .location(gl -> gl.latlon(ll -> ll
                                            .lat(req.getUserLat()).lon(req.getUserLng())))
                                    .order(SortOrder.Asc)
                                    .unit(DistanceUnit.Kilometers)
                                    .ignoreUnmapped(true))));
        };
    }

    private List<ShopSearchDocument> hitsToList(SearchResponse<ShopSearchDocument> resp) {
        return resp.hits().hits().stream()
                .map(Hit::source)
                .filter(doc -> doc != null)
                .toList();
    }

    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }
}
