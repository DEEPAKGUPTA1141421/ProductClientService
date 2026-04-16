package com.ProductClientService.ProductClientService.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Creates the search-intents-v1 Elasticsearch index on startup if it does not
 * already exist.
 *
 * Mapping highlights:
 *   keyword          → search_as_you_type (max 3-gram)
 *                      ES auto-creates ._2gram, ._3gram, ._index_prefix sub-fields
 *                      which are used by the bool_prefix multi_match for autocomplete.
 *   searchCount      → long — used as ranking signal in function_score
 *   clickCount       → long — used as ranking signal (weight > searchCount)
 *   filterPayload    → object, enabled=false (stored, not indexed or searched)
 *   imageUrl         → keyword, not analyzed, not indexed (pure display)
 *   suggestionType   → keyword (for future admin filtering)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer {

    static final String SEARCH_INTENTS_INDEX = "search-intents-v1";

    private final ElasticsearchClient esClient;

    @PostConstruct
    public void createSearchIntentsIndex() {
        try {
            boolean exists = esClient.indices()
                    .exists(ExistsRequest.of(r -> r.index(SEARCH_INTENTS_INDEX)))
                    .value();

            if (exists) {
                log.info("Index {} already exists — skipping creation", SEARCH_INTENTS_INDEX);
                return;
            }

            esClient.indices().create(c -> c
                    .index(SEARCH_INTENTS_INDEX)
                    .mappings(m -> m
                            .properties("keyword", p -> p
                                    .searchAsYouType(s -> s.maxShingleSize(3)))
                            .properties("searchCount", p -> p
                                    .long_(l -> l))
                            .properties("clickCount", p -> p
                                    .long_(l -> l))
                            .properties("filterPayload", p -> p
                                    .object(o -> o.enabled(false)))
                            .properties("imageUrl", p -> p
                                    .keyword(k -> k.index(false)))
                            .properties("suggestionType", p -> p
                                    .keyword(k -> k))
                    )
            );
            log.info("Created Elasticsearch index: {}", SEARCH_INTENTS_INDEX);

        } catch (Exception e) {
            log.warn("Could not initialize {} index: {}", SEARCH_INTENTS_INDEX, e.getMessage());
        }
    }
}
