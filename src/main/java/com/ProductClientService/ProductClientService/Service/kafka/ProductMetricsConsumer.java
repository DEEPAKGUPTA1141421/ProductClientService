package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Consumes product and order metric events from Kafka.
 * All listeners share the consumer group "product-metrics-group".
 *
 * Failure strategy: log and ack (best-effort metrics).
 * Metrics are business signals, not financial data — a missed
 * increment is acceptable; stalling the partition is not.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductMetricsConsumer {

    private final MetricsWriterService writer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product.viewed", groupId = "product-metrics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onProductViewed(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProductViewedEvent event = objectMapper.readValue(record.value(), ProductViewedEvent.class);
            writer.recordView(event.getProductId());
            log.debug("Recorded view for productId={}", event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.viewed: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "product.cart_added", groupId = "product-metrics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onCartAdded(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProductCartAddedEvent event = objectMapper.readValue(record.value(), ProductCartAddedEvent.class);
            writer.recordCartAdd(event.getProductId());
            log.debug("Recorded cart_add for productId={}", event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.cart_added: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "product.wishlisted", groupId = "product-metrics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onWishlisted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProductWishlistedEvent event = objectMapper.readValue(record.value(), ProductWishlistedEvent.class);
            if ("ADD".equals(event.getAction())) {
                writer.recordWishlistAdd(event.getProductId());
            } else {
                writer.recordWishlistRemove(event.getProductId());
            }
            log.debug("Recorded wishlist {} for productId={}", event.getAction(), event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process product.wishlisted: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.completed", groupId = "product-metrics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderCompleted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            OrderCompletedEvent event = objectMapper.readValue(record.value(), OrderCompletedEvent.class);
            if (event.getItems() != null) {
                for (var item : event.getItems()) {
                    writer.recordPurchase(item.getProductId(), item.getQuantity());
                }
            }
            log.debug("Recorded {} order items for orderId={}", event.getItems().size(), event.getOrderId());
        } catch (Exception e) {
            log.warn("Failed to process order.completed: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "order.returned", groupId = "product-metrics-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onOrderReturned(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            OrderReturnedEvent event = objectMapper.readValue(record.value(), OrderReturnedEvent.class);
            if (event.getItems() != null) {
                for (var item : event.getItems()) {
                    writer.recordReturn(item.getProductId(), item.getQuantity());
                }
            }
            log.debug("Recorded return for orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.warn("Failed to process order.returned: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
