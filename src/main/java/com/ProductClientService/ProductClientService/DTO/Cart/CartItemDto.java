package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private UUID id;
    private UUID productId;
    private UUID shopId;
    private int quantity;
    private double price; // price per unit
}
