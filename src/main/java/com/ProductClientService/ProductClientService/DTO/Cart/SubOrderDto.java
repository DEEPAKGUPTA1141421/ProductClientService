package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Represents the portion of a cart that belongs to a single seller/shop.
 *
 * The Order service must create one sub-order per SubOrderDto entry.
 * All financial fields are already computed and ready to use:
 *
 *   subTotal               = sum(item.price * item.quantity)
 *   itemLevelDiscount      = sum of per-item coupon discounts for this shop
 *   proportionalCartDiscount = cart-level coupon share allocated to this shop
 *                             (distributed proportionally by net sub-total)
 *   taxableAmount          = subTotal - itemLevelDiscount - proportionalCartDiscount
 *   gstCharge              = 18% of taxableAmount
 *   deliveryCharge         = 0 if taxableAmount > 500, else 50
 *   subOrderTotal          = taxableAmount + gstCharge + deliveryCharge
 *
 * Grand-total of the whole cart = sum(subOrderTotal) + serviceCharge (₹30 flat).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderDto {

    /** Seller / shop this sub-order belongs to. */
    private UUID shopId;

    /** Items in this sub-order (subset of cart items). */
    private List<CartItemDto> items;

    /** Raw total before any discounts. */
    private double subTotal;

    /** Sum of per-item coupon discounts for this shop. */
    private double itemLevelDiscount;

    /**
     * Cart-level coupon discount apportioned to this shop.
     * Calculated as: cartCouponDiscount × (shopNetSubTotal / totalNetSubTotal)
     */
    private double proportionalCartDiscount;

    /** Amount on which GST is applied (subTotal - itemDiscount - cartDiscount). */
    private double taxableAmount;

    /** 18% GST on taxableAmount. */
    private double gstCharge;

    /** Free if taxableAmount > ₹500, else ₹50. */
    private double deliveryCharge;

    /** taxableAmount + gstCharge + deliveryCharge */
    private double subOrderTotal;
}
