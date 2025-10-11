package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CartItemDto {
    private UUID id;
    private UUID productId;
    private UUID shopId;
    private int quantity;
    private double price;
    private UUID variantId;
    private String name;
    private String image;
    private String description;
}

// huhyuyb byguy gytguy hggy jhhhh njkh