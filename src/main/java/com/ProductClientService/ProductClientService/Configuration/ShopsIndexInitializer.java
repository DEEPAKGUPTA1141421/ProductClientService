package com.ProductClientService.ProductClientService.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ShopsIndexInitializer
 * ──────────────────────
 * Creates the "shops-v1" Elasticsearch index on startup if it does not exist.
 * Idempotent — safe to run on every boot.
 *
 * Index: shops-v1
 * ──────────────────────────────────────────────────────────────────────────────
 * shop_id         keyword          → exact match / term filter
 * display_name    text             → multi_match search
 *                 + .keyword       → exact / sort (ignoreAbove 256)
 *                 + .suggest       → search_as_you_type for autocomplete
 * legal_name      text             → fallback search field
 * tags            text             → boostable search field
 * city            keyword          → filter by city
 * category_id     keyword          → filter by category
 * category_name   text             → searchable label
 * status          keyword          → filter ACTIVE / INACTIVE
 * is_open         boolean          → filter open shops
 * location        geo_point        → geo_distance filter + sort by distance
 * avg_rating      float            → range filter (≥ 3.0 / 4.0 / 4.5) + sort
 * review_count    integer          → display; future ranking signal
 * ranking_score   float            → function_score boost (Phase 3)
 * logo_url        keyword[] (no idx) → array of media URLs, display only, never searched
 * created_at      date             → newest sort
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopsIndexInitializer {

    static final String SHOPS_INDEX = "shops-v1";

    private final ElasticsearchClient esClient;

    @PostConstruct
    public void initializeShopsIndex() {
        createShopsIndex();
        patchShopsIndexSuggest();
    }

    // ── shops-v1 ──────────────────────────────────────────────────────────────

    private void createShopsIndex() {
        try {
            if (indexExists(SHOPS_INDEX)) {
                if (hasCorrectGeoMapping(SHOPS_INDEX)) {
                    log.info("Index {} already exists with correct mapping — skipping creation", SHOPS_INDEX);
                    return;
                }
                log.warn("Index {} exists but location is not geo_point — dropping and recreating", SHOPS_INDEX);
                esClient.indices().delete(d -> d.index(SHOPS_INDEX));
            }

            esClient.indices().create(c -> c
                    .index(SHOPS_INDEX)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1"))
                    .mappings(m -> m
                            // ── identity ──────────────────────────────────────
                            .properties("shop_id",       p -> p.keyword(k -> k))
                            // ── searchable text ───────────────────────────────
                            .properties("display_name",  p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                            .properties("legal_name",    p -> p.text(t -> t))
                            .properties("tags",          p -> p.text(t -> t))
                            // ── category ──────────────────────────────────────
                            .properties("category_id",   p -> p.keyword(k -> k))
                            .properties("category_name", p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(128)))))
                            // ── location / city ───────────────────────────────
                            .properties("city",          p -> p.keyword(k -> k))
                            .properties("location",      p -> p.geoPoint(g -> g))
                            // ── status ────────────────────────────────────────
                            .properties("status",        p -> p.keyword(k -> k))
                            .properties("is_open",       p -> p.boolean_(b -> b))
                            // ── rating / ranking ──────────────────────────────
                            .properties("avg_rating",    p -> p.float_(f -> f))
                            .properties("review_count",  p -> p.integer(i -> i))
                            .properties("ranking_score", p -> p.float_(f -> f))
                            // ── display only ──────────────────────────────────
                            .properties("logo_url",      p -> p.keyword(k -> k.index(false)))
                            // ── timestamps ────────────────────────────────────
                            .properties("created_at",    p -> p.date(d -> d))));

            log.info("Created Elasticsearch index: {}", SHOPS_INDEX);

        } catch (Exception e) {
            log.warn("Could not initialize {} index: {}", SHOPS_INDEX, e.getMessage());
        }
    }

    /**
     * Additive patch: adds search_as_you_type sub-field on display_name
     * for autocomplete suggestions.
     *
     * Called on every boot; ES ignores additions for fields that already exist.
     * Safe to run against both fresh and existing indices.
     */
    private void patchShopsIndexSuggest() {
        try {
            esClient.indices().putMapping(m -> m
                    .index(SHOPS_INDEX)
                    .properties("display_name_suggest", p -> p.searchAsYouType(s -> s
                            .maxShingleSize(3))));
            log.info("Patched index {} with display_name_suggest (search_as_you_type)", SHOPS_INDEX);
        } catch (Exception e) {
            log.warn("Suggest mapping patch failed on {}: {}", SHOPS_INDEX, e.getMessage());
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private boolean indexExists(String index) throws Exception {
        return esClient.indices()
                .exists(ExistsRequest.of(r -> r.index(index)))
                .value();
    }

    private boolean hasCorrectGeoMapping(String index) {
        try {
            GetMappingResponse resp = esClient.indices().getMapping(m -> m.index(index));
            Property locationProp = resp.result().get(index).mappings().properties().get("location");
            return locationProp != null && locationProp.isGeoPoint();
        } catch (Exception e) {
            log.warn("Could not verify mapping for {}: {}", index, e.getMessage());
            return true; // assume correct to avoid accidental data loss
        }
    }
}
