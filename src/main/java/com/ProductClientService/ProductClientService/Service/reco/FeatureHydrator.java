package com.ProductClientService.ProductClientService.Service.reco;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import com.ProductClientService.ProductClientService.DTO.reco.RecoItemDto;
import com.ProductClientService.ProductClientService.DTO.search.ProductSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Turns a ranked list of productIds into compact UI DTOs.
 * Uses ES mget so a single round-trip hydrates up to 50 candidates; title,
 * price and thumbnail come from the denormalised products-v1 doc so the
 * hot path never touches Postgres.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureHydrator {

    private static final String INDEX = "products-v1";

    private final ElasticsearchClient es;

    public List<RecoItemDto> hydrate(List<String> productIds,
                                     Map<String, Double> scoreByProduct,
                                     Map<String, String> reasonByProduct) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        try {
            MgetResponse<ProductSearchDocument> resp = es.mget(m -> m
                    .index(INDEX)
                    .ids(productIds)
                    .sourceExcludes(List.of("text_embedding", "image_embedding")),
                    ProductSearchDocument.class);

            // Preserve input order — mget does not guarantee it on the client side.
            Map<String, ProductSearchDocument> byId = new HashMap<>();
            for (MultiGetResponseItem<ProductSearchDocument> item : resp.docs()) {
                if (item.isResult() && item.result().found() && item.result().source() != null) {
                    ProductSearchDocument d = item.result().source();
                    byId.put(d.getProductId(), d);
                }
            }

            List<RecoItemDto> out = new ArrayList<>(productIds.size());
            for (String pid : productIds) {
                ProductSearchDocument d = byId.get(pid);
                if (d == null) continue;                // product may have been retired
                if (!"LIVE".equals(d.getStep())) continue;
                if (!d.isInStock()) continue;
                String thumb = (d.getImages() != null && !d.getImages().isEmpty()) ? d.getImages().get(0) : null;
                out.add(RecoItemDto.builder()
                        .productId(d.getProductId())
                        .title(d.getName())
                        .pricePaise(d.getMinPricePaise())
                        .discountPct(d.getDiscountPercent() > 0 ? d.getDiscountPercent() : null)
                        .thumbnailUrl(thumb)
                        .avgRating(d.getAvgRating() > 0 ? d.getAvgRating() : null)
                        .score(scoreByProduct.get(pid))
                        .reason(reasonByProduct.get(pid))
                        .build());
            }
            return out;
        } catch (Exception e) {
            log.warn("hydrate failed for {} ids: {}", productIds.size(), e.getMessage());
            return List.of();
        }
    }
}
