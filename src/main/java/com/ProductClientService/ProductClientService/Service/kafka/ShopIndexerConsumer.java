package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.SellerLiveEvent;
import com.ProductClientService.ProductClientService.Service.ShopIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ShopIndexerConsumer
 * ────────────────────
 * Handles "seller.live" events and indexes (or re-indexes) the seller
 * as a shop document in the "shops-v1" Elasticsearch index. Delivered via
 * Kafka or Redis depending on app.messaging.provider.
 *
 * Event published by: SellerService when a seller's status transitions to ACTIVE.
 *
 * Failure strategy: log and continue.
 * ShopIndexer.indexSeller() is idempotent — a failed index attempt can be
 * retried via the admin re-index endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopIndexerConsumer {

    static final String TOPIC = "seller.live";
    static final String GROUP = "shop-es-indexer-group";

    private final ShopIndexer shopIndexer;
    private final ObjectMapper objectMapper;

    public void handleSellerLive(String payload) {
        try {
            SellerLiveEvent event = objectMapper.readValue(payload, SellerLiveEvent.class);
            log.info("Received seller.live event for sellerId={}, indexing to shops-v1",
                    event.getSellerId());
            shopIndexer.indexSeller(event.getSellerId());
        } catch (Exception e) {
            log.warn("Failed to process seller.live event: {}", e.getMessage());
        }
    }
}
