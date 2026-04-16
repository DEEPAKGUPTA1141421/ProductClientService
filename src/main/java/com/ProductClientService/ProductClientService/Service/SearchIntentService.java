package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchIntentDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SearchIntentService
 * ────────────────────
 * All reads and writes go to the "search-intents-v1" Elasticsearch index.
 * No database table involved.
 *
 * autocomplete(prefix)
 *   Multi-match bool_prefix query on search_as_you_type sub-fields.
 *   Ranked by function_score: log1p(clickCount) * 2.0 + log1p(searchCount) * 1.5
 *   so "clicked" suggestions float to the top of the list.
 *
 * recordClick(keyword)
 *   Returns the stored filterPayload, then async-increments clickCount in ES.
 *   The frontend sends back this filterPayload to /products/search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentService {

    static final String INDEX = "search-intents-v1";

    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    // ── Autocomplete ──────────────────────────────────────────────────────────────

    /**
     * Returns up to 10 ranked suggestions for the given prefix.
     * Each suggestion includes keyword, imageUrl, suggestionType, and filterPayload.
     */
    public ApiResponse<Object> autocomplete(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return new ApiResponse<>(false, "Keyword required", null, 400);
        }

        try {
            var response = esClient.search(s -> s
                    .index(INDEX)
                    .size(10)
                    .query(q -> q
                            .functionScore(fs -> fs
                                    .query(inner -> inner
                                            .multiMatch(mm -> mm
                                                    .query(prefix.trim())
                                                    .fields(
                                                            "keyword",
                                                            "keyword._2gram",
                                                            "keyword._3gram",
                                                            "keyword._index_prefix"
                                                    )
                                                    .type(TextQueryType.BoolPrefix)
                                            )
                                    )
                                    // clickCount has higher weight — a suggestion users act on
                                    // is more valuable than one they only see
                                    .functions(fn -> fn
                                            .fieldValueFactor(fvf -> fvf
                                                    .field("clickCount")
                                                    .modifier(FieldValueFactorModifier.Log1p)
                                                    .factor(2.0)
                                                    .missing(0.0)
                                            )
                                    )
                                    .functions(fn -> fn
                                            .fieldValueFactor(fvf -> fvf
                                                    .field("searchCount")
                                                    .modifier(FieldValueFactorModifier.Log1p)
                                                    .factor(1.5)
                                                    .missing(0.0)
                                            )
                                    )
                                    .scoreMode(FunctionScoreMode.Sum)
                                    .boostMode(FunctionBoostMode.Multiply)
                            )
                    ),
                    SearchIntentDocument.class
            );

            List<SearchIntentDto> dtos = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .map(doc -> new SearchIntentDto(
                            doc.getKeyword(),
                            doc.getKeyword(),
                            doc.getImageUrl(),
                            doc.getSuggestionType(),
                            doc.getFilterPayload() != null
                                    ? objectMapper.valueToTree(doc.getFilterPayload())
                                    : null
                    ))
                    .toList();

            // Async — does not delay the response
            dtos.forEach(dto -> incrementSearchCountAsync(dto.id()));

            return new ApiResponse<>(true, "Suggestions fetched", dtos, 200);

        } catch (Exception e) {
            log.warn("ES autocomplete failed for prefix='{}': {}", prefix, e.getMessage());
            return new ApiResponse<>(true, "Suggestions fetched", List.of(), 200);
        }
    }

    // ── Record click ──────────────────────────────────────────────────────────────

    /**
     * Called when a user taps a suggestion.
     * Returns the stored filterPayload so the frontend can call /products/search.
     *
     * Flow:
     *   frontend taps suggestion (keyword = doc ID)
     *   → POST /search-intent/click?keyword=<keyword>
     *   → receives { filterPayload: { categoryId, filters: { ... } } }
     *   → calls GET /products/search with those params
     */
    public ApiResponse<Object> recordClick(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ApiResponse<>(false, "Keyword required", null, 400);
        }
        try {
            GetResponse<SearchIntentDocument> res = esClient.get(g -> g
                    .index(INDEX)
                    .id(keyword.trim().toLowerCase()),
                    SearchIntentDocument.class
            );

            if (!res.found() || res.source() == null) {
                return new ApiResponse<>(false, "Intent not found", null, 404);
            }

            SearchIntentDocument doc = res.source();
            incrementClickCountAsync(keyword.trim().toLowerCase());

            JsonNode filterPayload = doc.getFilterPayload() != null
                    ? objectMapper.valueToTree(doc.getFilterPayload())
                    : null;

            return new ApiResponse<>(true, "Filter payload resolved",
                    new SearchIntentClickResponse(doc.getKeyword(), filterPayload), 200);

        } catch (Exception e) {
            log.warn("recordClick failed for keyword='{}': {}", keyword, e.getMessage());
            return new ApiResponse<>(false, "Something went wrong", null, 500);
        }
    }

    // ── Async counter updates (fire-and-forget) ───────────────────────────────────

    @Async
    public void incrementSearchCountAsync(String keyword) {
        updateCounter(keyword, "searchCount");
    }

    @Async
    public void incrementClickCountAsync(String keyword) {
        updateCounter(keyword, "clickCount");
    }

    private void updateCounter(String keyword, String field) {
        try {
            esClient.update(u -> u
                    .index(INDEX)
                    .id(keyword)
                    .script(sc -> sc
                            .inline(i -> i
                                    .lang("painless")
                                    .source("ctx._source." + field + " = (ctx._source." + field + " ?: 0L) + 1")
                            )
                    ),
                    SearchIntentDocument.class
            );
        } catch (Exception e) {
            log.debug("Failed to increment {} for keyword='{}': {}", field, keyword, e.getMessage());
        }
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────────

    /**
     * id = keyword (also the ES doc ID).
     * Frontend stores this to call recordClick later.
     */
    public record SearchIntentDto(
            String id,
            String keyword,
            String imageUrl,
            String suggestionType,
            JsonNode filterPayload
    ) {}

    public record SearchIntentClickResponse(
            String keyword,
            JsonNode filterPayload
    ) {}
}
