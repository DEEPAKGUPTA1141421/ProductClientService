package com.ProductClientService.ProductClientService.DTO.Cart;

import java.util.UUID;

/**
 * A non-blocking validation signal attached to CartResponseDto.
 *
 * The caller (Order service or UI) must inspect this list before
 * creating an order. Items flagged as OUT_OF_STOCK / ITEM_UNAVAILABLE
 * MUST be removed before checkout can proceed.
 * INSUFFICIENT_STOCK means the requested qty exceeds available stock.
 * COUPON_EXPIRED / CART_COUPON_EXPIRED means the discount was dropped
 * from the calculation and the totals reflect that.
 */
public record CartValidationIssue(
        Type type,
        UUID cartItemId,   // null for cart-level issues (e.g. CART_COUPON_EXPIRED)
        UUID productId,    // null for cart-level issues
        String message
) {
    public enum Type {
        OUT_OF_STOCK,
        INSUFFICIENT_STOCK,
        ITEM_UNAVAILABLE,
        COUPON_EXPIRED,
        CART_COUPON_EXPIRED
    }
}
