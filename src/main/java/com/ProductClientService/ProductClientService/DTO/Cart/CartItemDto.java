package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private UUID id;
    private UUID productId;
    private UUID shopId;
    private int quantity;
    private double price;
}
