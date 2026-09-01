package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.SellerNotificationEventDto;
import com.ProductClientService.ProductClientService.Service.messaging.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes seller-facing notification events (low stock, new review) to the
 * shared "notification.events" topic, consumed by
 * OrderPaymentNotificationService's NotificationEventListener. This reuses
 * that service's already-built DB-persisted + FCM notification pipeline
 * instead of building a second one here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SellerNotificationPublisher {

    private static final String TOPIC = "notification.events";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public void publish(UUID sellerId, String category, String title, String body,
                         String actionUrl, String referenceId, Map<String, String> pushData) {
        try {
            SellerNotificationEventDto event = SellerNotificationEventDto.builder()
                    .userId(sellerId)
                    .category(category)
                    .title(title)
                    .body(body)
                    .actionUrl(actionUrl)
                    .referenceId(referenceId)
                    .pushData(pushData)
                    .sourceService("product-client-service")
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            eventPublisher.publish(TOPIC, sellerId.toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // Never let a notification failure break the calling business flow.
            log.error("Failed to publish seller notification event: sellerId={}, category={}, error={}",
                    sellerId, category, e.getMessage());
        }
    }
}
