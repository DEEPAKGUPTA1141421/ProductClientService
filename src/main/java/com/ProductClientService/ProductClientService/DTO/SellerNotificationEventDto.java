package com.ProductClientService.ProductClientService.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors OrderPaymentNotificationService's NotificationEvent JSON shape.
 * Published to the shared Kafka/Redis topic "notification.events" so that
 * service's already-built NotificationEventListener -> NotificationDispatcher
 * pipeline (DB persistence + FCM push + preferences) picks it up — avoids
 * duplicating that pipeline here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationEventDto {
    private UUID userId;
    private String category;
    private String title;
    private String body;
    private String actionUrl;
    private String referenceId;
    private Map<String, String> pushData;
    private String sourceService;
    @Builder.Default
    private Instant createdAt = Instant.now();
    private String idempotencyKey;
}
