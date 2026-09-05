package com.ProductClientService.ProductClientService.Service.section;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import co.elastic.clients.json.JsonData;
import com.ProductClientService.ProductClientService.DTO.sections.SectionItemResponseDto;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.SectionItem;
import com.ProductClientService.ProductClientService.Model.SectionItemRepository;
import com.ProductClientService.ProductClientService.Service.reco.ColdStartResolver;
import com.ProductClientService.ProductClientService.Service.reco.FeatureHydrator;
import com.ProductClientService.ProductClientService.Service.reco.RecoOrchestrator;
import com.ProductClientService.ProductClientService.DTO.reco.RecoContext;
import com.ProductClientService.ProductClientService.DTO.reco.RecoItemDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionHydrator {

    private final ElasticsearchClient es;
    private final RecoOrchestrator recoOrchestrator;
    private final ColdStartResolver coldStartResolver;
    private final FeatureHydrator featureHydrator;
    private final SectionItemRepository sectionItemRepository;

    private static final String PRODUCTS_INDEX = "products-v1";

    public List<SectionItemResponseDto> hydrate(Section section, UUID userId) {
        try {
            return switch (section.getDataSource()) {
                case STATIC -> hydrateStaticItems(section);
                case RECO_FOR_YOU -> hydrateRecoForYou(section, userId);
                case RECO_TRENDING -> hydrateTrending(section);
                case CATEGORY_TOP -> hydrateCategoryTop(section);
                case SEARCH_QUERY -> hydrateSearchQuery(section);
            };
        } catch (Exception e) {
            log.warn("Section hydration failed for section={}, dataSource={}: {}",
                    section.getId(), section.getDataSource(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SectionItemResponseDto> hydrateStaticItems(Section section) {
        // Fetch directly rather than trusting section.getItems() — that
        // association is populated by a LEFT JOIN FETCH + DISTINCT query run
        // on the calling thread (SectionService.getPage, not @Transactional),
        // then read here inside a CompletableFuture.supplyAsync() on a
        // different thread. That combination has silently produced an empty
        // collection for PRODUCT items in practice; a direct repository call
        // has no such dependency on the originating session/thread.
        List<SectionItem> items = sectionItemRepository.findBySectionId(section.getId());

        List<String> productIds = items.stream()
                .filter(item -> item.getItemType().name().equals("PRODUCT"))
                .map(SectionItem::getItemRefId)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return items.stream()
                    .map(this::itemToDto)
                    .collect(Collectors.toList());
        }

        Map<String, ProductSearchDocument> productsById = fetchProductsByIds(productIds);

        return items.stream()
                .sorted(Comparator.comparingInt(SectionItem::getPosition))
                .map(item -> {
                    if (item.getItemType().name().equals("PRODUCT")) {
                        ProductSearchDocument doc = productsById.get(item.getItemRefId());
                        return doc != null ? toProductDto(item, doc) : itemToDto(item);
                    }
                    return itemToDto(item);
                })
                .collect(Collectors.toList());
    }

    private List<SectionItemResponseDto> hydrateRecoForYou(Section section, UUID userId) {
        if (userId == null) {
            return hydrateTrending(section);
        }

        int k = extractK(section, 20);
        var recoResponse = recoOrchestrator.forYou(userId, k, RecoContext.HOME, null);

        return recoResponse.getItems().stream()
                .map(recoItem -> SectionItemResponseDto.builder()
                        .itemType("PRODUCT")
                        .itemRefId(recoItem.getProductId())
                        .productId(recoItem.getProductId())
                        .title(recoItem.getTitle())
                        .pricePaise(recoItem.getPricePaise())
                        .discountPct(recoItem.getDiscountPct())
                        .thumbnailUrl(recoItem.getThumbnailUrl())
                        .avgRating(recoItem.getAvgRating())
                        .score(recoItem.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private List<SectionItemResponseDto> hydrateTrending(Section section) {
        int k = extractK(section, 20);
        var candidates = coldStartResolver.resolve(k, Collections.emptyList());
        var items = featureHydrator.hydrate(
                candidates.productIds,
                candidates.scores,
                candidates.reasons
        );

        return items.stream()
                .map(recoItem -> SectionItemResponseDto.builder()
                        .itemType("PRODUCT")
                        .itemRefId(recoItem.getProductId())
                        .productId(recoItem.getProductId())
                        .title(recoItem.getTitle())
                        .pricePaise(recoItem.getPricePaise())
                        .discountPct(recoItem.getDiscountPct())
                        .thumbnailUrl(recoItem.getThumbnailUrl())
                        .avgRating(recoItem.getAvgRating())
                        .score(recoItem.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * dataParams: {"categoryId":"<uuid>", "sortBy":"rating|discount|newest|popularity",
     *              "minPricePaise":..., "maxPricePaise":..., "minDiscount":..., "k":10}
     * Falls back to the section's own `category` name when categoryId is absent.
     */
    private List<SectionItemResponseDto> hydrateCategoryTop(Section section) {
        JsonNode params = section.getDataParams();
        int k = extractK(section, 20);

        List<Query> filters = new ArrayList<>();
        filters.add(liveInStockFilter());

        String categoryId = textParam(params, "categoryId");
        if (categoryId != null) {
            filters.add(term("category_id", categoryId));
        } else {
            String categoryName = textParam(params, "categoryName");
            if (categoryName == null) categoryName = section.getCategory();
            if (categoryName != null && !categoryName.isBlank()) {
                filters.add(term("category_name.keyword", categoryName));
            }
        }

        applyPriceAndDiscountFilters(filters, params);

        String sortBy = textParam(params, "sortBy");
        return runProductQuery(filters, List.of(), sortBy, k);
    }

    /**
     * dataParams: {"query":"kurti", "categoryId":"<uuid>", "minPricePaise":..., "maxPricePaise":...,
     *              "minDiscount":..., "sortBy":"rel|price_asc|price_desc|rating|newest|discount", "k":10}
     */
    private List<SectionItemResponseDto> hydrateSearchQuery(Section section) {
        JsonNode params = section.getDataParams();
        int k = extractK(section, 20);

        List<Query> filters = new ArrayList<>();
        filters.add(liveInStockFilter());

        String categoryId = textParam(params, "categoryId");
        if (categoryId != null) {
            filters.add(term("category_id", categoryId));
        }

        applyPriceAndDiscountFilters(filters, params);

        List<Query> shouldClauses = new ArrayList<>();
        String keyword = textParam(params, "query");
        if (keyword != null && !keyword.isBlank()) {
            final String kw = keyword.trim();
            shouldClauses.add(Query.of(q -> q.multiMatch(m -> m
                    .query(kw)
                    .fields(Arrays.asList("name^3", "brand_name^2", "description",
                            "category_name", "tags"))
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO"))));
            shouldClauses.add(Query.of(q -> q.multiMatch(m -> m
                    .query(kw)
                    .fields(Arrays.asList("name^4", "brand_name^2", "category_name"))
                    .type(TextQueryType.PhrasePrefix)
                    .maxExpansions(50))));
        }

        String sortBy = textParam(params, "sortBy");
        return runProductQuery(filters, shouldClauses, sortBy, k);
    }

    // ── Shared ES query helpers ───────────────────────────────────────────────

    private static final String PRODUCTS_INDEX_SEARCH = PRODUCTS_INDEX;

    private Query liveInStockFilter() {
        Query stepFilter = Query.of(q -> q.terms(t -> t
                .field("step")
                .terms(tv -> tv.value(List.of(FieldValue.of("LIVE"), FieldValue.of("4"))))));
        return stepFilter;
    }

    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(value))));
    }

    private void applyPriceAndDiscountFilters(List<Query> filters, JsonNode params) {
        Long minPricePaise = longParam(params, "minPricePaise");
        Long maxPricePaise = longParam(params, "maxPricePaise");
        if (minPricePaise != null || maxPricePaise != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.field("min_price_paise");
                if (minPricePaise != null) r.gte(JsonData.of(minPricePaise));
                if (maxPricePaise != null) r.lte(JsonData.of(maxPricePaise));
                return r;
            })));
        }
        Integer minDiscount = intParam(params, "minDiscount");
        if (minDiscount != null && minDiscount > 0) {
            filters.add(Query.of(q -> q.range(r -> r
                    .field("discount_percent")
                    .gte(JsonData.of(minDiscount)))));
        }
        filters.add(term("in_stock", "true"));
    }

    private List<SectionItemResponseDto> runProductQuery(
            List<Query> filters, List<Query> shouldClauses, String sortBy, int k) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder().filter(filters);
            if (!shouldClauses.isEmpty()) {
                boolBuilder.should(shouldClauses).minimumShouldMatch("1");
            }
            Query boolQuery = Query.of(q -> q.bool(boolBuilder.build()));

            String sort = sortBy == null || sortBy.isBlank() ? "rel" : sortBy;

            SearchRequest req = switch (sort) {
                case "price_asc" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("min_price_paise").order(SortOrder.Asc))));
                case "price_desc" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("min_price_paise").order(SortOrder.Desc))));
                case "rating" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("avg_rating").order(SortOrder.Desc))));
                case "newest" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("created_at").order(SortOrder.Desc))));
                case "discount" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("discount_percent").order(SortOrder.Desc))));
                case "popularity" -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.field(f -> f.field("ranking_score").order(SortOrder.Desc))));
                default -> SearchRequest.of(s -> s.index(PRODUCTS_INDEX_SEARCH)
                        .query(boolQuery).size(k)
                        .sort(so -> so.score(sc -> sc.order(SortOrder.Desc)))
                        .sort(so -> so.field(f -> f.field("ranking_score").order(SortOrder.Desc))));
            };

            SearchResponse<ProductSearchDocument> resp =
                    es.search(req, ProductSearchDocument.class);

            return resp.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(Objects::nonNull)
                    .map(this::docToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Section product query failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private SectionItemResponseDto docToDto(ProductSearchDocument doc) {
        String thumb = (doc.getImages() != null && !doc.getImages().isEmpty())
                ? doc.getImages().get(0)
                : null;

        return SectionItemResponseDto.builder()
                .itemType("PRODUCT")
                .itemRefId(doc.getProductId())
                .productId(doc.getProductId())
                .title(doc.getName())
                .pricePaise(doc.getMinPricePaise())
                .discountPct(doc.getDiscountPercent() > 0 ? doc.getDiscountPercent() : null)
                .thumbnailUrl(thumb)
                .avgRating(doc.getAvgRating() > 0 ? doc.getAvgRating() : null)
                .build();
    }

    private String textParam(JsonNode params, String key) {
        if (params == null || !params.hasNonNull(key)) return null;
        return params.get(key).asText(null);
    }

    private Long longParam(JsonNode params, String key) {
        if (params == null || !params.hasNonNull(key)) return null;
        return params.get(key).asLong();
    }

    private Integer intParam(JsonNode params, String key) {
        if (params == null || !params.hasNonNull(key)) return null;
        return params.get(key).asInt();
    }

    private Map<String, ProductSearchDocument> fetchProductsByIds(List<String> productIds) {
        try {
            MgetResponse<ProductSearchDocument> response = es.mget(m -> m
                    .index(PRODUCTS_INDEX)
                    .ids(productIds)
                    .sourceExcludes(Arrays.asList("text_embedding", "image_embedding")),
                    ProductSearchDocument.class);

            Map<String, ProductSearchDocument> result = new HashMap<>();
            for (MultiGetResponseItem<ProductSearchDocument> item : response.docs()) {
                if (item.isResult() && item.result().found() && item.result().source() != null) {
                    ProductSearchDocument doc = item.result().source();
                    result.put(doc.getProductId(), doc);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch products from ES: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private SectionItemResponseDto toProductDto(SectionItem item, ProductSearchDocument doc) {
        String thumb = (doc.getImages() != null && !doc.getImages().isEmpty())
                ? doc.getImages().get(0)
                : null;

        return SectionItemResponseDto.builder()
                .itemType("PRODUCT")
                .itemRefId(item.getItemRefId())
                .productId(doc.getProductId())
                .title(doc.getName())
                .pricePaise(doc.getMinPricePaise())
                .discountPct(doc.getDiscountPercent() > 0 ? doc.getDiscountPercent() : null)
                .thumbnailUrl(thumb)
                .avgRating(doc.getAvgRating() > 0 ? doc.getAvgRating() : null)
                .metadata(item.getMetadata())
                .build();
    }

    private SectionItemResponseDto itemToDto(SectionItem item) {
        return SectionItemResponseDto.builder()
                .itemType(item.getItemType().name())
                .itemRefId(item.getItemRefId())
                .metadata(item.getMetadata())
                .build();
    }

    private int extractK(Section section, int defaultK) {
        if (section.getDataParams() != null && section.getDataParams().has("k")) {
            return section.getDataParams().get("k").asInt(defaultK);
        }
        return defaultK;
    }
}
