package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemDto {
    private UUID id;
    private UUID productId;
    private UUID shopId;
    private int quantity;
    private double price;
    /** Base price before an active discount. 0 when there is no discount (use price for display). */
    private double mrp;
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
     *
     * NOTE: serializes as JSON key "available" (Jackson strips the isXxx()
     * accessor's "is" prefix) — OrderPaymentNotificationService's internal
     * CartItemDto.available field, and the user_app frontend, both key off
     * "available", not "isAvailable". Keep this in sync if renamed.
     */
    private boolean isAvailable;
}
