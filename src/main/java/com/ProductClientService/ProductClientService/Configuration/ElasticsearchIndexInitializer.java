package com.ProductClientService.ProductClientService.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Creates required Elasticsearch indices on startup if they do not already
 * exist.
 *
 * Indices managed:
 * search-intents-v1 — autocomplete suggestions (search_as_you_type)
 * products-v1 — live product search index (filtered / ranked / sorted)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer {

    static final String SEARCH_INTENTS_INDEX = "search-intents-v1";
    static final String PRODUCTS_INDEX = "products-v1";

    private final ElasticsearchClient esClient;

    @PostConstruct
    public void initializeIndices() {
        createSearchIntentsIndex();
        createProductsIndex();
        patchProductsIndexForSimilarity();
    }

    // ── Additive patch: dense_vector + popularity_7d for similarity search ────
    //
    // Called on every boot; ES ignores additions for fields that already exist.
    // Backfill of text_embedding/image_embedding is owned by the offline
    // embedding DAG in reco-serving/ — until that lands, values are absent and
    // SimilarityService falls back to more_like_this.
    //
    // Dimensions:
    //   text_embedding  = 384  (multilingual-e5-base)
    //   image_embedding = 512  (OpenCLIP ViT-B/32)
    private void patchProductsIndexForSimilarity() {
        try {
            esClient.indices().putMapping(m -> m
                    .index(PRODUCTS_INDEX)
                    .properties("text_embedding", p -> p.denseVector(d -> d
                            .dims(384).index(true).similarity("cosine")))
                    .properties("image_embedding", p -> p.denseVector(d -> d
                            .dims(512).index(true).similarity("cosine")))
                    .properties("popularity_7d", p -> p.rankFeature(r -> r)));
            log.info("Patched index {} with dense_vector + popularity_7d fields", PRODUCTS_INDEX);
        } catch (Exception e) {
            log.warn("Similarity mapping patch failed on {}: {}", PRODUCTS_INDEX, e.getMessage());
        }
    }

    // ── search-intents-v1 ─────────────────────────────────────────────────────

    private void createSearchIntentsIndex() {
        try {
            if (indexExists(SEARCH_INTENTS_INDEX)) {
                log.info("Index {} already exists \u00f9 skipping creation", SEARCH_INTENTS_INDEX);
                return;
            }
            esClient.indices().create(c -> c
                    .index(SEARCH_INTENTS_INDEX)
                    .mappings(m -> m
                            .properties("keyword", p -> p
                                    .searchAsYouType(s -> s.maxShingleSize(3)))
                            .properties("searchCount", p -> p.long_(l -> l))
                            .properties("clickCount", p -> p.long_(l -> l))
                            .properties("filterPayload", p -> p.object(o -> o.enabled(false)))
                            .properties("imageUrl", p -> p.keyword(k -> k.index(false)))
                            .properties("suggestionType", p -> p.keyword(k -> k))));
            log.info("Created Elasticsearch index: {}", SEARCH_INTENTS_INDEX);
        } catch (Exception e) {
            log.warn("Could not initialize {} index: {}", SEARCH_INTENTS_INDEX, e.getMessage());
        }
    }

    // ── products-v1 ───────────────────────────────────────────────────────────

    /**
     * Mapping highlights:
     * name, description → text (full-text search + fuzziness)
     * brand_name → text + keyword sub-field (search + exact filter)
     * category_id, brand_id → keyword (term filter)
     * step, in_stock → keyword / boolean (mandatory filters)
     * min_price_paise → long (fast range filter — no CAST)
     * discount_percent → integer (range filter for "X% off" badge)
     * avg_rating → float (range filter + sort)
     * ranking_score → float (function_score booster)
     * attributes → nested — prevents cross-attribute false positives
     * e.g. color=red AND size=M must match on same object
     * images → keyword, index=false (display only, not searched)
     * created_at → date (new-arrivals filter + sort)
     */
    public void createProductsIndex() {
        try {
          log.info("step1");
            if (indexExists(PRODUCTS_INDEX)) {
                log.info("Index {} already exists — skipping creation", PRODUCTS_INDEX);
                return;
            }
            log.info("step3");
            
            esClient.indices().create(c -> c
                    .index(PRODUCTS_INDEX)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1"))
                    .mappings(m -> m
                            // ── identity ─────────────────────────────────────
                            .properties("product_id", p -> p.keyword(k -> k))
                            .properties("variant_id", p -> p.keyword(k -> k))
                            .properties("seller_id", p -> p.keyword(k -> k))
                            // ── searchable text ───────────────────────────────
                            .properties("name", p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                            .properties("description", p -> p.text(t -> t))
                            .properties("tags", p -> p.text(t -> t))
                            // ── category / brand ──────────────────────────────
                            .properties("category_id", p -> p.keyword(k -> k))
                            .properties("category_name", p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(128)))))
                            .properties("brand_id", p -> p.keyword(k -> k))
                            .properties("brand_name", p -> p.text(t -> t
                                    .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(128)))))
                            // ── lifecycle / stock ─────────────────────────────
                            .properties("step", p -> p.keyword(k -> k))
                            .properties("in_stock", p -> p.boolean_(b -> b))
                            // ── pricing ───────────────────────────────────────
                            .properties("min_price_paise", p -> p.long_(l -> l))
                            .properties("original_price_paise", p -> p.long_(l -> l))
                            .properties("discount_percent", p -> p.integer(i -> i))
                            // ── delivery ──────────────────────────────────────
                            .properties("delivery_days", p -> p.integer(i -> i))
                            .properties("free_delivery", p -> p.boolean_(b -> b))
                            // ── media ─────────────────────────────────────────
                            .properties("images", p -> p.keyword(k -> k.index(false)))
                            // ── ratings ───────────────────────────────────────
                            .properties("avg_rating", p -> p.float_(f -> f))
                            .properties("review_count", p -> p.integer(i -> i))
                            // ── ranking / metrics ─────────────────────────────
                            .properties("ranking_score", p -> p.float_(f -> f))
                            .properties("number_of_orders", p -> p.long_(l -> l))
                            .properties("number_of_purchases", p -> p.long_(l -> l))
                            .properties("conversion_rate", p -> p.float_(f -> f))
                            .properties("click_through_rate", p -> p.float_(f -> f))
                            .properties("wishlist_count", p -> p.long_(l -> l))
                            .properties("cart_add_count", p -> p.long_(l -> l))
                            .properties("return_rate", p -> p.float_(f -> f))
                            .properties("recent_sales_7d", p -> p.integer(i -> i))
                            // ── timestamps ────────────────────────────────────
                            .properties("created_at", p -> p.date(d -> d))
                            // ── nested attributes (critical for correct filtering) ──
                            .properties("attributes", p -> p.nested(n -> n
                                    .properties("name", np -> np.keyword(k -> k))
                                    .properties("value", np -> np.keyword(k -> k))))));
            log.info("Created Elasticsearch index: {}", PRODUCTS_INDEX);
        } catch (Exception e) {
            log.warn("Could not initialize {} index: {}", PRODUCTS_INDEX, e.getMessage());
        }
    }

    private boolean indexExists(String index) throws Exception {
        return esClient.indices()
                .exists(ExistsRequest.of(r -> r.index(index)))
                .value();
    }
}
