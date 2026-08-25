package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.ProductLiveEvent;
import com.ProductClientService.ProductClientService.Service.SearchIntentGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles product.live events and triggers Elasticsearch search-intent indexing.
 * Delivered via Kafka or Redis depending on app.messaging.provider.
 *
 * Decoupled from the HTTP request path — the seller's "make product live" call
 * returns immediately; intent generation happens asynchronously.
 *
 * Failure strategy: log and continue.
 * A missed intent generation is recoverable (re-trigger via the /test endpoint),
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentIndexerConsumer {

    static final String TOPIC = "product.live";
    static final String GROUP  = "search-intent-indexer-group";

    private final SearchIntentGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    public void handleProductLive(String payload) {
        try {
            ProductLiveEvent event = objectMapper.readValue(payload, ProductLiveEvent.class);
            log.info("Received product.live event for productId={}", event.getProductId());
            generatorService.generateForProduct(event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.live event: {}", e.getMessage());
        }
    }
}
