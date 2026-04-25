package com.ProductClientService.ProductClientService.Service.similarity;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import com.ProductClientService.ProductClientService.DTO.similarity.SimilarProductDto;
import com.ProductClientService.ProductClientService.DTO.similarity.SimilarProductsResponse;
import com.ProductClientService.ProductClientService.DTO.similarity.SimilarityVariant;
import com.ProductClientService.ProductClientService.Service.reco.RecoMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Resolves similar-product recommendations for the product detail page.
 *
 * Two code paths, chosen per request:
 *   1. more_like_this on name/description/tags + category filter
 *      — works today against the existing products-v1 index, no embeddings needed.
 *   2. kNN on text_embedding (gated behind reco.similarity.vectorsEnabled)
 *      — activates once the offline embedding DAG backfills vectors.
 *
 * The response is cached in Redis keyed by (productId, variant, modelVersion).
 * TTL is intentionally long (6h) because embedding space only changes when
 * the model bumps version, which invalidates the key naturally.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityService {

    private static final String INDEX = "products-v1";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final int MAX_K = 50;

    private final ElasticsearchClient es;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final RecoMetrics metrics;

    @Value("${reco.similarity.vectorsEnabled:false}")
    private boolean vectorsEnabled;

    @Value("${reco.similarity.textModelVersion:mlt_v1}")
    private String textModelVersion;

    public SimilarProductsResponse findSimilar(UUID productId, int k, SimilarityVariant variant) {
        Timer.Sample sample = Timer.start(metrics.registry());
        try {
            return findSimilarInner(productId, k, variant);
        } finally {
            sample.stop(metrics.simLatency);
        }
    }

    private SimilarProductsResponse findSimilarInner(UUID productId, int k, SimilarityVariant variant) {
        int topK = Math.min(Math.max(k, 1), MAX_K);
        String modelVersion = vectorsEnabled ? "knn_" + textModelVersion : "mlt_v1";
        String cacheKey = "sim:prod:" + productId + ":" + variant + ":" + modelVersion;

        SimilarProductsResponse cached = readCache(cacheKey);
        if (cached != null) {
            metrics.simCacheHit.increment();
            return cached;
        }

        ProductSearchDocument source = fetchSource(productId);
        if (source == null) {
            metrics.simEmpty.increment();
            return empty(productId, variant, modelVersion);
        }

        boolean useVectors = vectorsEnabled && hasVector(productId);
        (useVectors ? metrics.simVectorPath : metrics.simMltPath).increment();
        List<SimilarProductDto> items = useVectors
                ? searchByVector(source, topK, variant)
                : searchByMoreLikeThis(source, topK, variant);
        if (items.isEmpty()) metrics.simEmpty.increment();

        SimilarProductsResponse resp = SimilarProductsResponse.builder()
                .productId(productId.toString())
                .variant(variant)
                .modelVersion(modelVersion)
                .items(items)
                .build();

        writeCache(cacheKey, resp);
        return resp;
    }

    // ── ES calls ─────────────────────────────────────────────────────────────

    private ProductSearchDocument fetchSource(UUID productId) {
        try {
            GetResponse<ProductSearchDocument> r = es.get(
                    g -> g.index(INDEX).id(productId.toString()),
                    ProductSearchDocument.class);
            return r.found() ? r.source() : null;
        } catch (Exception e) {
            log.warn("ES get failed for productId={}: {}", productId, e.getMessage());
            return null;
        }
    }

    /** Cheap existence probe; if the doc has no indexed text_embedding, skip kNN. */
    private boolean hasVector(UUID productId) {
        try {
            SearchResponse<Object> r = es.search(s -> s
                    .index(INDEX)
                    .size(0)
                    .query(q -> q.bool(b -> b
                            .filter(f -> f.ids(i -> i.values(productId.toString())))
                            .filter(f -> f.exists(x -> x.field("text_embedding"))))),
                    Object.class);
            return r.hits().total() != null && r.hits().total().value() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private List<SimilarProductDto> searchByMoreLikeThis(
            ProductSearchDocument source, int k, SimilarityVariant variant) {
        try {
            final String sourceId = source.getProductId();
            final String categoryId = source.getCategoryId();
            final long priceCeiling = variant == SimilarityVariant.SIMILAR_CHEAPER
                    ? Math.round(source.getMinPricePaise() * 0.85)
                    : Long.MAX_VALUE;

            SearchResponse<ProductSearchDocument> r = es.search(s -> s
                    .index(INDEX)
                    .size(k)
                    .source(sf -> sf.filter(ff -> ff.excludes("text_embedding", "image_embedding")))
                    .query(q -> q.bool(b -> {
                        b.must(m -> m.moreLikeThis(mlt -> mlt
                                .fields("name", "description", "tags", "brand_name", "category_name")
                                .like(l -> l.document(d -> d.index(INDEX).id(sourceId)))
                                .minTermFreq(1)
                                .maxQueryTerms(25)
                                .minDocFreq(2)));
                        b.mustNot(mn -> mn.ids(i -> i.values(sourceId)));
                        b.filter(f -> f.term(t -> t.field("step").value("LIVE")));
                        b.filter(f -> f.term(t -> t.field("in_stock").value(true)));
                        if (categoryId != null && variant != SimilarityVariant.COMPLETE_LOOK) {
                            b.filter(f -> f.term(t -> t.field("category_id").value(categoryId)));
                        }
                        if (variant == SimilarityVariant.SIMILAR_CHEAPER) {
                            b.filter(f -> f.range(rg -> rg.field("min_price_paise").lte(co.elastic.clients.json.JsonData.of(priceCeiling))));
                        }
                        return b;
                    })),
                    ProductSearchDocument.class);

            return toDtos(r);
        } catch (Exception e) {
            log.warn("more_like_this failed for productId={}: {}", source.getProductId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<SimilarProductDto> searchByVector(
            ProductSearchDocument source, int k, SimilarityVariant variant) {
        try {
            final String sourceId = source.getProductId();
            final String categoryId = source.getCategoryId();
            final long priceCeiling = variant == SimilarityVariant.SIMILAR_CHEAPER
                    ? Math.round(source.getMinPricePaise() * 0.85)
                    : Long.MAX_VALUE;

            Query filter = Query.of(q -> q.bool(b -> {
                b.filter(f -> f.term(t -> t.field("step").value("LIVE")));
                b.filter(f -> f.term(t -> t.field("in_stock").value(true)));
                b.mustNot(mn -> mn.ids(i -> i.values(sourceId)));
                if (categoryId != null && variant != SimilarityVariant.COMPLETE_LOOK) {
                    b.filter(f -> f.term(t -> t.field("category_id").value(categoryId)));
                }
                if (variant == SimilarityVariant.SIMILAR_CHEAPER) {
                    b.filter(f -> f.range(rg -> rg.field("min_price_paise").lte(co.elastic.clients.json.JsonData.of(priceCeiling))));
                }
                return b;
            }));

            // Fetch source vector then run kNN. We intentionally request a
            // generic Map (ProductSearchDocument drops unknown fields, including
            // the embedding) with a source filter that returns only the vector.
            @SuppressWarnings({"rawtypes", "unchecked"})
            GetResponse<java.util.Map> vecResp = (GetResponse<java.util.Map>) (GetResponse) es.get(
                    g -> g.index(INDEX).id(sourceId)
                          .sourceIncludes(java.util.List.of("text_embedding")),
                    java.util.Map.class);
            if (!vecResp.found() || vecResp.source() == null) return Collections.emptyList();
            Object vecObj = vecResp.source().get("text_embedding");
            if (!(vecObj instanceof List<?> rawVec) || rawVec.isEmpty()) return Collections.emptyList();
            List<Float> vec = new ArrayList<>(rawVec.size());
            for (Object o : rawVec) vec.add(((Number) o).floatValue());

            SearchResponse<ProductSearchDocument> r = es.search(s -> s
                    .index(INDEX)
                    .size(k)
                    .source(sf -> sf.filter(ff -> ff.excludes("text_embedding", "image_embedding")))
                    .knn(kn -> kn
                            .field("text_embedding")
                            .queryVector(vec)
                            .k(k)
                            .numCandidates(Math.min(500, k * 10))
                            .filter(filter)),
                    ProductSearchDocument.class);

            return toDtos(r);
        } catch (Exception e) {
            log.warn("kNN failed for productId={}: {}", source.getProductId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Mapping + cache ──────────────────────────────────────────────────────

    private List<SimilarProductDto> toDtos(SearchResponse<ProductSearchDocument> r) {
        List<SimilarProductDto> out = new ArrayList<>(r.hits().hits().size());
        for (Hit<ProductSearchDocument> hit : r.hits().hits()) {
            ProductSearchDocument d = hit.source();
            if (d == null) continue;
            String thumb = (d.getImages() != null && !d.getImages().isEmpty()) ? d.getImages().get(0) : null;
            out.add(SimilarProductDto.builder()
                    .productId(d.getProductId())
                    .title(d.getName())
                    .pricePaise(d.getMinPricePaise())
                    .discountPct(d.getDiscountPercent() > 0 ? d.getDiscountPercent() : null)
                    .thumbnailUrl(thumb)
                    .avgRating(d.getAvgRating() > 0 ? d.getAvgRating() : null)
                    .score(hit.score())
                    .build());
        }
        return out;
    }

    private SimilarProductsResponse empty(UUID productId, SimilarityVariant v, String modelVersion) {
        return SimilarProductsResponse.builder()
                .productId(productId.toString())
                .variant(v)
                .modelVersion(modelVersion)
                .items(Collections.emptyList())
                .build();
    }

    private SimilarProductsResponse readCache(String key) {
        try {
            String raw = redis.opsForValue().get(key);
            return raw != null ? mapper.readValue(raw, SimilarProductsResponse.class) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCache(String key, SimilarProductsResponse resp) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(resp), CACHE_TTL);
        } catch (Exception e) {
            log.debug("sim cache write skipped: {}", e.getMessage());
        }
    }
}
