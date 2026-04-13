package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Published to topic: product.viewed */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewedEvent {
    private UUID productId;
    private UUID userId;       // null for guests
    private UUID categoryId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
