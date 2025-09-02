package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.Model.Cart;
import com.ProductClientService.ProductClientService.Model.CartItem;
import com.ProductClientService.ProductClientService.Model.Coupon;
import com.ProductClientService.ProductClientService.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final CouponRepository couponRepo;

    @Transactional
    public ApiResponse<Object> addItem(UUID userId, CartItemRequest req) {
        try {
            Cart cart = cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                    .orElseGet(() -> cartRepo.save(
                            Cart.builder()
                                    .userId(userId)
                                    .status(Cart.Status.ACTIVE)
                                    .build()));

            // merge if same product+variant exists
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(req.getProductId())
                            && Objects.equals(i.getVariantId(), req.getVariantId()))
                    .findFirst();

            if (existing.isPresent()) {
                CartItem it = existing.get();
                it.setQuantity(it.getQuantity() + Math.max(1, req.getQuantity()));
                it.setPriceAtAddition(req.getUnitPrice()); // ✅ req.getUnitPrice() must return String (paise)
            } else {
                CartItem it = CartItem.builder()
                        .cart(cart)
                        .productId(req.getProductId())
                        .variantId(req.getVariantId())
                        .quantity(Math.max(1, req.getQuantity()))
                        .priceAtAddition(req.getUnitPrice()) // ✅ ensure String paise
                        .metadata(req.getMetadata())
                        .lineDiscount("0") // ✅ always initialize as string
                        .build();
                cart.getItems().add(it);
            }

            recompute(cart);
            return new ApiResponse<>(true, "Added To Cart", cartRepo.save(cart), 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> updateQuantity(UUID userId, UUID itemId, int qty) {
        try {
            Cart cart = mustGetActiveCart(userId);
            CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not in cart"));
            if (qty <= 0) {
                cart.getItems().remove(item);
                itemRepo.delete(item);
            } else {
                item.setQuantity(qty);
            }
            recompute(cart);
            return new ApiResponse<>(true, "Quantity Change", cartRepo.save(cart), 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public Cart removeItem(UUID userId, UUID itemId) {
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not in cart"));
        cart.getItems().remove(item);
        itemRepo.delete(item);
        recompute(cart);
        return cartRepo.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCart(UUID userId) {
        return cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                .orElseGet(() -> Cart.builder().userId(userId).status(Cart.Status.ACTIVE).items(List.of()).build());
    }

    @Transactional
    public Cart clearCart(UUID userId) {
        Cart cart = mustGetActiveCart(userId);
        cart.getItems().clear();
        cart.getAppliedCartCoupons().clear();
        cart.setItemLevelDiscount("0");
        cart.setCartLevelDiscount("0");
        recompute(cart);
        return cartRepo.save(cart);
    }

    // ---------- Coupons ----------
    @Transactional
    public Cart applyItemCoupon(UUID userId, ApplyCouponRequest req) {
        if (req.getItemId() == null)
            throw new IllegalArgumentException("itemId required for item coupon");
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(req.getItemId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found"));

        Coupon coupon = couponRepo.findByCodeIgnoreCaseAndActiveTrue(req.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon"));
        validateCouponWindow(coupon);

        if (coupon.getScope() != Coupon.Scope.ITEM)
            throw new IllegalArgumentException("Coupon is not item-scope");

        // Validate applicability (here you can query product->brand/category if needed)
        // For demo: only PRODUCT applicability strictly checks productId
        switch (coupon.getApplicability()) {
            case PRODUCT -> {
                if (!item.getProductId().equals(coupon.getProductId()))
                    throw new IllegalArgumentException("Coupon not applicable on this product");
            }
            case BRAND, CATEGORY, CART_ALL, CART_TOTAL -> {
                /* accept (extend with brand/category lookups) */ }
        }

        // compute discount for that item
        BigDecimal base = new BigDecimal(item.getPriceAtAddition())
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal disc = computeDiscount(base, coupon.getDiscountType(), coupon.getDiscountValue());
        item.setAppliedCoupon(coupon);
        item.setLineDiscount(disc.toPlainString());

        recompute(cart);
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart removeItemCoupon(UUID userId, UUID itemId) {
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found"));
        item.setAppliedCoupon(null);
        item.setLineDiscount("0");
        recompute(cart);
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart applyCartCoupon(UUID userId, String code) {
        Cart cart = mustGetActiveCart(userId);
        Coupon coupon = couponRepo.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon"));
        validateCouponWindow(coupon);
        if (coupon.getScope() != Coupon.Scope.CART)
            throw new IllegalArgumentException("Coupon is not cart-scope");

        // validate min cart total if needed
        BigDecimal currentSubTotal = new BigDecimal(cart.getSubTotal())
                .subtract(new BigDecimal(cart.getItemLevelDiscount()));
        if (coupon.getApplicability() == Coupon.Applicability.CART_TOTAL
                && coupon.getMinCartTotal() != null
                && currentSubTotal.compareTo(coupon.getMinCartTotal()) < 0) {
            throw new IllegalArgumentException("Cart total below minimum for this coupon");
        }
        cart.getAppliedCartCoupons().add(coupon);
        recompute(cart);
        return cartRepo.save(cart);
    }

    @Transactional
    public Cart removeCartCoupon(UUID userId, String code) {
        Cart cart = mustGetActiveCart(userId);
        couponRepo.findByCodeIgnoreCaseAndActiveTrue(code).ifPresent(c -> cart.getAppliedCartCoupons().remove(c));
        recompute(cart);
        return cartRepo.save(cart);
    }

    // ---------- Helpers ----------
    private Cart mustGetActiveCart(UUID userId) {
        return cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active cart for user"));
    }

    private void validateCouponWindow(Coupon coupon) {
        ZonedDateTime now = ZonedDateTime.now();
        if (Boolean.FALSE.equals(coupon.getActive()))
            throw new IllegalArgumentException("Coupon inactive");
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt()))
            throw new IllegalArgumentException("Coupon not started");
        if (coupon.getEndsAt() != null && now.isAfter(coupon.getEndsAt()))
            throw new IllegalArgumentException("Coupon expired");
    }

    private BigDecimal computeDiscount(BigDecimal base, Coupon.DiscountType type, BigDecimal value) {
        if (value == null || base.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;
        return switch (type) {
            case FLAT -> value.min(base);
            case PERCENT -> base.multiply(value).divide(BigDecimal.valueOf(100));
        };
    }

    private void recompute(Cart cart) {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal itemDisc = BigDecimal.ZERO;

        for (CartItem it : cart.getItems()) {
            // Convert stored string (paise) to BigDecimal
            BigDecimal price = new BigDecimal(it.getPriceAtAddition() == null ? "0" : it.getPriceAtAddition());
            BigDecimal lineBase = price.multiply(BigDecimal.valueOf(it.getQuantity()));

            sub = sub.add(lineBase);

            BigDecimal lineDiscount = new BigDecimal(it.getLineDiscount() == null ? "0" : it.getLineDiscount());
            itemDisc = itemDisc.add(lineDiscount);
        }

        // cart-level coupons applied on (sub - itemDisc)
        BigDecimal cartBase = sub.subtract(itemDisc);
        BigDecimal cartDisc = BigDecimal.ZERO;
        for (Coupon c : cart.getAppliedCartCoupons()) {
            cartDisc = cartDisc.add(
                    computeDiscount(cartBase, c.getDiscountType(), c.getDiscountValue()));
        }

        BigDecimal taxable = cartBase.subtract(cartDisc).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(new BigDecimal("0.18")); // example 18% GST

        // Store back as strings (paise)
        cart.setSubTotal(sub.toPlainString());
        cart.setItemLevelDiscount(itemDisc.toPlainString());
        cart.setCartLevelDiscount(cartDisc.toPlainString());
        cart.setTax(tax.toPlainString());
        cart.setGrandTotal(taxable.add(tax).toPlainString());
    }
}
// huyy jggyut