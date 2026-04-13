package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published to topic: order.returned
 * Source: OrderPaymentNotification microservice on return approval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReturnedEvent {
    private UUID orderId;
    private UUID userId;
    private List<OrderItem> items;
    private String reason;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
