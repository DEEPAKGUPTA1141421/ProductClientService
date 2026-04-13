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
 * EventPublisherService
 * ──────────────────────
 * Thin wrapper over KafkaTemplate that:
 *  1. Serializes event POJOs to JSON strings (existing KafkaConfig uses StringSerializer)
 *  2. Catches all exceptions so a Kafka outage never breaks the hot path
 *  3. Publishes asynchronously so the HTTP response is not delayed
 *
 * Every publish method is @Async — the caller returns immediately.
 * The background thread calls KafkaTemplate.send() without blocking the request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public static final String TOPIC_VIEWED      = "product.viewed";
    public static final String TOPIC_CART_ADDED  = "product.cart_added";
    public static final String TOPIC_WISHLISTED  = "product.wishlisted";
    public static final String TOPIC_ORDER_DONE  = "order.completed";
    public static final String TOPIC_ORDER_RET   = "order.returned";

    @Async
    public void publishProductViewed(UUID productId, UUID userId, UUID categoryId) {
        publish(TOPIC_VIEWED, ProductViewedEvent.builder()
                .productId(productId)
                .userId(userId)
                .categoryId(categoryId)
                .build());
    }

    @Async
    public void publishCartAdded(UUID productId, UUID variantId, UUID userId) {
        publish(TOPIC_CART_ADDED, ProductCartAddedEvent.builder()
                .productId(productId)
                .variantId(variantId)
                .userId(userId)
                .build());
    }

    @Async
    public void publishWishlistAdd(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId)
                .userId(userId)
                .action("ADD")
                .build());
    }

    @Async
    public void publishWishlistRemove(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId)
                .userId(userId)
                .action("REMOVE")
                .build());
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private void publish(String topic, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            // Metrics are best-effort — a Kafka outage must not fail the API call
            log.warn("Failed to publish event to topic={}: {}", topic, e.getMessage());
        }
    }
}
