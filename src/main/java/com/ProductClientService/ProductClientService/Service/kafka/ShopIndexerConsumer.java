package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.SellerLiveEvent;
import com.ProductClientService.ProductClientService.Service.ShopIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * ShopIndexerConsumer
 * ────────────────────
 * Listens for "seller.live" events and indexes (or re-indexes) the seller
 * as a shop document in the "shops-v1" Elasticsearch index.
 *
 * Event published by: SellerService when a seller's status transitions to ACTIVE.
 *
 * Failure strategy: log and ack.
 * ShopIndexer.indexSeller() is idempotent — a failed index attempt can be
 * retried via the admin re-index endpoint without stalling the partition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopIndexerConsumer {

    static final String TOPIC = "seller.live";
    static final String GROUP = "shop-es-indexer-group";

    private final ShopIndexer shopIndexer;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSellerLive(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            SellerLiveEvent event = objectMapper.readValue(record.value(), SellerLiveEvent.class);
            log.info("Received seller.live event for sellerId={}, indexing to shops-v1",
                    event.getSellerId());
            shopIndexer.indexSeller(event.getSellerId());
        } catch (Exception e) {
            log.warn("Failed to process seller.live event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
