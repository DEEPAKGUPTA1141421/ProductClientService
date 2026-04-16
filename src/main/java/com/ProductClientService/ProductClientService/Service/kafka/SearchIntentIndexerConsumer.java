package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.ProductLiveEvent;
import com.ProductClientService.ProductClientService.Service.SearchIntentGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Consumes product.live events and triggers Elasticsearch search-intent indexing.
 *
 * Decoupled from the HTTP request path — the seller's "make product live" call
 * returns immediately; intent generation happens asynchronously via Kafka.
 *
 * Failure strategy: log and ack.
 * A missed intent generation is recoverable (re-trigger via the /test endpoint),
 * whereas stalling the partition would block all future product.live events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentIndexerConsumer {

    static final String TOPIC = "product.live";
    static final String GROUP  = "search-intent-indexer-group";

    private final SearchIntentGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProductLive(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProductLiveEvent event = objectMapper.readValue(record.value(), ProductLiveEvent.class);
            log.info("Received product.live event for productId={}", event.getProductId());
            generatorService.generateForProduct(event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.live event: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
