package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ShopRatingUpdater
 * ──────────────────
 * Keeps the shops-v1 Elasticsearch document's avg_rating and review_count
 * in sync whenever a product review is saved.
 *
 * Called async by ReviewEventConsumer after it persists a new ProductRating.
 * One DB call → one ES partial update — never blocks the Kafka ack.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopRatingUpdater {

    private static final String SHOP_INDEX = "shops-v1";

    private final ProductRatingRepository ratingRepository;
    private final ElasticsearchClient esClient;

    /**
     * Recomputes the seller's aggregate rating and pushes a partial update
     * to the shops-v1 document (keyed by sellerId).
     *
     * @param sellerId the seller whose shop document should be updated
     */
    @Async
    public void syncShopRating(UUID sellerId) {
        try {
            List<Object[]> rows = ratingRepository.findSellerRatingSummary(sellerId);
            if (rows == null || rows.isEmpty() || rows.get(0)[0] == null) {
                return; // no reviews yet — nothing to update
            }

            Object[] row       = rows.get(0);
            double avgRating   = ((Number) row[0]).doubleValue();
            long reviewCount   = ((Number) row[1]).longValue();

            // Partial update — only touch avg_rating and review_count
            esClient.update(u -> u
                    .index(SHOP_INDEX)
                    .id(sellerId.toString())
                    .doc(Map.of(
                            "avg_rating",   Math.round(avgRating * 10.0) / 10.0,
                            "review_count", reviewCount
                    )),
                    Object.class);

            log.debug("Updated shop rating for sellerId={}: avg={} count={}",
                    sellerId, avgRating, reviewCount);

        } catch (Exception e) {
            log.warn("Shop rating sync failed for sellerId={}: {}", sellerId, e.getMessage());
        }
    }
}
