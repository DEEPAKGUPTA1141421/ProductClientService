package com.ProductClientService.ProductClientService.Service.reco;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Cold-start candidate generation.
 *
 * Tier 1 — user has recent session clicks → return popular LIVE products
 *          in the same category as the most-recent click (fast, high-signal).
 * Tier 2 — no clicks yet → global popularity by ranking_score, in-stock only.
 *
 * A future enhancement is geo-weighted popularity: boost docs whose
 * recent_sales_7d is concentrated in the requester's PIN code. Deferred
 * until we wire PIN into the JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColdStartResolver {

    private static final String INDEX = "products-v1";

    private final ElasticsearchClient es;

    public static class Candidates {
        public final List<String> productIds;
        public final Map<String, Double> scores;
        public final Map<String, String> reasons;
        public final String modelVersion;

        public Candidates(List<String> ids, Map<String, Double> scores,
                          Map<String, String> reasons, String modelVersion) {
            this.productIds = ids;
            this.scores = scores;
            this.reasons = reasons;
            this.modelVersion = modelVersion;
        }
    }

    public Candidates resolve(int k, List<String> sessionClicks) {
        String anchor = sessionClicks != null && !sessionClicks.isEmpty() ? sessionClicks.get(0) : null;
        String reason = anchor != null ? "based_on_recent_views" : "popular_now";
        String anchorCategory = anchor != null ? lookupCategory(anchor) : null;

        try {
            SearchResponse<ProductSearchDocument> r = es.search(s -> s
                    .index(INDEX)
                    .size(k)
                    .source(sf -> sf.filter(ff -> ff.excludes("text_embedding", "image_embedding")))
                    .query(q -> q.bool(b -> {
                        b.filter(f -> f.term(t -> t.field("step").value("LIVE")));
                        b.filter(f -> f.term(t -> t.field("in_stock").value(true)));
                        if (anchorCategory != null) {
                            b.filter(f -> f.term(t -> t.field("category_id").value(anchorCategory)));
                        }
                        if (anchor != null) {
                            b.mustNot(mn -> mn.ids(i -> i.values(anchor)));
                        }
                        return b;
                    }))
                    .sort(sr -> sr.field(f -> f.field("ranking_score").order(SortOrder.Desc))),
                    ProductSearchDocument.class);

            List<String> ids = new ArrayList<>();
            Map<String, Double> scores = new HashMap<>();
            Map<String, String> reasons = new HashMap<>();
            for (Hit<ProductSearchDocument> h : r.hits().hits()) {
                if (h.source() == null) continue;
                String pid = h.source().getProductId();
                ids.add(pid);
                scores.put(pid, h.source().getRankingScore());
                reasons.put(pid, reason);
            }
            return new Candidates(ids, scores, reasons, "cold_start_v1");
        } catch (Exception e) {
            log.warn("cold-start ES query failed: {}", e.getMessage());
            return new Candidates(List.of(), Map.of(), Map.of(), "cold_start_v1");
        }
    }

    private String lookupCategory(String productId) {
        try {
            var r = es.get(g -> g.index(INDEX).id(productId)
                    .sourceIncludes(List.of("category_id")),
                    ProductSearchDocument.class);
            return r.found() && r.source() != null ? r.source().getCategoryId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
