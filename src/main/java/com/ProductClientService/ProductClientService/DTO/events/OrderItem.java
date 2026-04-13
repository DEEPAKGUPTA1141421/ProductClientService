package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** One line-item inside an order event. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private UUID productId;
    private UUID variantId;
    private int quantity;
}
