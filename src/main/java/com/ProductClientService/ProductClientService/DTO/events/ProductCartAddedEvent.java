package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Published to topic: product.cart_added */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCartAddedEvent {
    private UUID productId;
    private UUID variantId;
    private UUID userId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
