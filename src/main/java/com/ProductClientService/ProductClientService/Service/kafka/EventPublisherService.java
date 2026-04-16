package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;


/**
 * Thin async wrapper over KafkaTemplate.
 * Every publish method is @Async — the HTTP response is never delayed by Kafka.
 * All exceptions are swallowed: a Kafka outage must not fail the API call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    public static final String TOPIC_VIEWED     = "product.viewed";
    public static final String TOPIC_CART_ADDED = "product.cart_added";
    public static final String TOPIC_WISHLISTED = "product.wishlisted";
    public static final String TOPIC_ORDER_DONE = "order.completed";
    public static final String TOPIC_ORDER_RET  = "order.returned";
    public static final String TOPIC_PRODUCT_LIVE = "product.live";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Async
    public void publishProductViewed(UUID productId, UUID userId, UUID categoryId) {
        publish(TOPIC_VIEWED, ProductViewedEvent.builder()
                .productId(productId).userId(userId).categoryId(categoryId).build());
    }

    @Async
    public void publishCartAdded(UUID productId, UUID variantId, UUID userId) {
        publish(TOPIC_CART_ADDED, ProductCartAddedEvent.builder()
                .productId(productId).variantId(variantId).userId(userId).build());
    }

    @Async
    public void publishWishlistAdd(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId).userId(userId).action("ADD").build());
    }

    @Async
    public void publishWishlistRemove(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId).userId(userId).action("REMOVE").build());
    }

    /**
     * Published when a product transitions to LIVE status.
     * Consumed by SearchIntentIndexerConsumer to generate and index
     * search intents into Elasticsearch.
     */
    @Async
    public void publishProductLive(UUID productId) {
        publish(TOPIC_PRODUCT_LIVE, ProductLiveEvent.builder()
                .productId(productId).build());
    }

    private void publish(String topic, Object event) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to publish to topic={}: {}", topic, e.getMessage());
        }
    }
}
