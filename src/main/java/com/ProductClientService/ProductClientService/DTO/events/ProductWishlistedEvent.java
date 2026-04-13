package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Published to topic: product.wishlisted */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWishlistedEvent {
    private UUID productId;
    private UUID userId;
    /** "ADD" or "REMOVE" */
    private String action;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
