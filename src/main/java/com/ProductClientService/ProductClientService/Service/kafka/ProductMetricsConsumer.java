package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles product and order metric events, delivered via Kafka or Redis
 * depending on app.messaging.provider (see KafkaMessagingListenerConfig /
 * RedisMessagingListenerConfig for the wiring).
 *
 * Failure strategy: log and swallow (best-effort metrics).
 * Metrics are business signals, not financial data — a missed
 * increment is acceptable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMetricsConsumer {

    private final MetricsWriterService writer;
    private final ObjectMapper objectMapper;

    public void handleProductViewed(String payload) {
        try {
            ProductViewedEvent event = objectMapper.readValue(payload, ProductViewedEvent.class);
            writer.recordView(event.getProductId());
            log.debug("Recorded view for productId={}", event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.viewed: {}", e.getMessage());
        }
    }

    public void handleCartAdded(String payload) {
        try {
            ProductCartAddedEvent event = objectMapper.readValue(payload, ProductCartAddedEvent.class);
            writer.recordCartAdd(event.getProductId());
            log.debug("Recorded cart_add for productId={}", event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.cart_added: {}", e.getMessage());
        }
    }

    public void handleWishlisted(String payload) {
        try {
            ProductWishlistedEvent event = objectMapper.readValue(payload, ProductWishlistedEvent.class);
            if ("ADD".equals(event.getAction())) {
                writer.recordWishlistAdd(event.getProductId());
            } else {
                writer.recordWishlistRemove(event.getProductId());
            }
            log.debug("Recorded wishlist {} for productId={}", event.getAction(), event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.wishlisted: {}", e.getMessage());
        }
    }

    public void handleOrderCompleted(String payload) {
        try {
            OrderCompletedEvent event = objectMapper.readValue(payload, OrderCompletedEvent.class);
            if (event.getItems() != null) {
                for (var item : event.getItems()) {
                    writer.recordPurchase(item.getProductId(), item.getQuantity());
                }
            }
            log.debug("Recorded {} order items for orderId={}", event.getItems().size(), event.getOrderId());
        } catch (Exception e) {
            log.warn("Failed to process order.completed: {}", e.getMessage());
        }
    }

    public void handleOrderReturned(String payload) {
        try {
            OrderReturnedEvent event = objectMapper.readValue(payload, OrderReturnedEvent.class);
            if (event.getItems() != null) {
                for (var item : event.getItems()) {
                    writer.recordReturn(item.getProductId(), item.getQuantity());
                }
            }
            log.debug("Recorded return for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.warn("Failed to process order.returned: {}", e.getMessage());
        }
    }
}
