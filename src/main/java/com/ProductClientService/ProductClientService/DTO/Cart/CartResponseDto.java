package com.ProductClientService.ProductClientService.DTO.Cart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Full cart snapshot returned to clients and the internal Order service.
 *
 * Financial summary (cart level):
 *   totalAmount      = sum(item.price × item.quantity) — before any discounts
 *   totalDiscount    = itemLevelDiscount + cartCouponDiscount
 *   serviceCharge    = ₹30 flat per cart
 *   deliveryCharge   = sum of per-shop delivery charges
 *   gstCharge        = sum of per-shop GST charges
 *   grandTotal       = sum(subOrder.subOrderTotal) + serviceCharge
 *
 * subOrders groups items by shopId with per-shop financials already
 * computed. The Order service must create one sub-order per entry.
 * It is populated on the internal endpoint (/internal/v1/cart/{userId})
 * used by the order/booking service, but omitted (null) on the customer-
 * facing endpoint (/api/v1/cart/get-cart) — the flat `items` list there
 * already carries everything the app UI needs, and shipping subOrders
 * too would duplicate every item a second time in the response body.
 *
 * validationIssues is non-null and may be empty. The Order service
 * MUST reject checkout if any issue of type OUT_OF_STOCK or
 * ITEM_UNAVAILABLE is present.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponseDto {

    // ── Identity ──────────────────────────────────────────────────────────────
    private UUID cartId;
    private UUID userId;
    private String status;

    // ── Items (flat list — also present inside each SubOrderDto) ──────────────
    private List<CartItemDto> items;

    // ── Sub-orders (one per shop) ─────────────────────────────────────────────
    private List<SubOrderDto> subOrders;

    // ── Cart-level financials ─────────────────────────────────────────────────
    /** Raw total before any discounts: sum(price × qty). */
    private double totalAmount;

    /** itemLevelDiscount + cartCouponDiscount. */
    private double totalDiscount;

    /** Flat ₹30 per cart. */
    private double serviceCharge;

    /** Sum of per-shop delivery charges. */
    private double deliveryCharge;

    /** Sum of per-shop GST charges. */
    private double gstCharge;

    /** sum(subOrder.subOrderTotal) + serviceCharge. */
    private double grandTotal;

    // ── Coupon ────────────────────────────────────────────────────────────────
    /** Code of the applied cart-level coupon, null if none. */
    private String cartCoupon;

    /** Absolute discount amount from the cart-level coupon (as stored string). */
    private String cartLineDiscount;

    // ── Validation ────────────────────────────────────────────────────────────
    /**
     * Non-blocking signals. Always non-null; may be empty.
     * OUT_OF_STOCK / ITEM_UNAVAILABLE must block checkout.
     * INSUFFICIENT_STOCK / COUPON_EXPIRED are warnings.
     */
    private List<CartValidationIssue> validationIssues;

    // ── Config ────────────────────────────────────────────────────────────────
    /** app_config["membership_offer"] value (title/subtitle/buttonText/price/active), null if not configured. */
    private Object membershipOffer;

    /** Whether the user has added the membership add-on to this cart. */
    private boolean membershipAdded;

    /** membershipOffer.price, included in grandTotal when membershipAdded is true. */
    private double membershipCharge;
}
