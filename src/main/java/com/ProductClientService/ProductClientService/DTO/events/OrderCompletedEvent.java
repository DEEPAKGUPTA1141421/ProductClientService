package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published to topic: order.completed
 * Source: OrderPaymentNotification microservice on payment success.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private UUID orderId;
    private UUID userId;
    private List<OrderItem> items;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
