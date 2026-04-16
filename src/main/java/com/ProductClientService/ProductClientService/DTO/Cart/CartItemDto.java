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
    private String appliedCoupon;
    private String discountLineAmount;

    /** Current stock units available for this variant. */
    private int stockAvailable;

    /**
     * False when stock == 0 or stock < quantity.
     * The Order service must reject checkout if any item is unavailable.
     */
    private boolean isAvailable;
}
