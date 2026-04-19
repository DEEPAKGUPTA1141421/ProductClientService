package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.ProductLiveEvent;
import com.ProductClientService.ProductClientService.Service.ElasticsearchProductIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Consumes product.live events and indexes the product into the
 * "products-v1" Elasticsearch index so it appears in search results.
 *
 * Runs in a separate consumer group from SearchIntentIndexerConsumer
 * so both consumers receive every product.live event independently.
 *
 * Failure strategy: log and ack.
 * A failed index attempt is recoverable — the batch reindex job or
 * a manual re-trigger can backfill the document without stalling the partition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexerConsumer {

    static final String TOPIC = "product.live";
    static final String GROUP = "product-es-indexer-group";

    private final ElasticsearchProductIndexer productIndexer;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProductLive(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProductLiveEvent event = objectMapper.readValue(record.value(), ProductLiveEvent.class);
            log.info("Received product.live event for productId={}, indexing to products-v1",
                    event.getProductId());
            productIndexer.indexProduct(event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.live event for ES indexing: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
