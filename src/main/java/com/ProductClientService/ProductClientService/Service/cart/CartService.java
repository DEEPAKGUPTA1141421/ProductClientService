package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemDto;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartResponseDto;
import com.ProductClientService.ProductClientService.DTO.Cart.CouponResponseDto;
import com.ProductClientService.ProductClientService.Model.Cart;
import com.ProductClientService.ProductClientService.Model.CartItem;
import com.ProductClientService.ProductClientService.Model.Coupon;
import com.ProductClientService.ProductClientService.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final CouponRepository couponRepo;

    private final ProductRepository productRepository;

    private final ProductVariantRepository variantRepository;

    @Transactional
    public ApiResponse<Object> addItem(UUID userId, CartItemRequest req) {
        try {
            Cart cart = cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                    .orElseGet(() -> cartRepo.save(
                            Cart.builder()
                                    .userId(userId)
                                    .status(Cart.Status.ACTIVE)
                                    .build()));
            List<CartItem> items = cart.getItems();
            if (items == null) {
                items = new ArrayList<>();
                cart.setItems(items);
            }
            // merge if same product+variant exists
            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(req.getProductId())
                            && Objects.equals(i.getVariantId(), req.getVariantId()))
                    .findFirst();

            if (existing.isPresent()) {
                CartItem it = existing.get();
                it.setQuantity(it.getQuantity() + Math.max(1, req.getQuantity()));
            } else {
                CartItem it = CartItem.builder()
                        .cart(cart)
                        .productId(req.getProductId())
                        .variantId(req.getVariantId())
                        .quantity(Math.max(1, req.getQuantity()))
                        .metadata(req.getMetadata())
                        .lineDiscount("0") // ✅ always initialize as string
                        .build();
                cart.getItems().add(it);
            }

            recompute(cart);
            cart = cartRepo.save(cart);
            return new ApiResponse<>(true, "Added To Cart", cart, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> updateQuantity(UUID userId, UUID itemId, int qty) {
        try {
            Cart cart = mustGetActiveCart(userId);

            CartItem item = cart.getItems().stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Item not in cart"));

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
    public ApiResponse<Object> removeItem(UUID userId, UUID itemId) {
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not in cart"));
        cart.getItems().remove(item);
        itemRepo.delete(item);
        recompute(cart);
        return new ApiResponse<>(true, "Removed Item from Cart", cartRepo.save(cart), 201);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getCart(UUID userId) {

        System.out.println("we receive the call");
        // Find active cart for user
        try {
            Cart cart = cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE)
                    .orElseGet(() -> Cart.builder()
                            .userId(userId)
                            .status(Cart.Status.ACTIVE)
                            .items(List.of())
                            .build());
            System.out.println("Cart" + cart.toString());

            // Convert CartItems -> CartItemDto
            List<CartItemDto> itemDtos = cart.getItems().stream()
                    .map(item -> {
                        System.out.println("Cart Item" + item.getProductId());
                        UUID shopId = productRepository.findSellerIdByProductId(item.getProductId());
                        System.out.println("shopId" + shopId);
                        UUID variantId = item.getVariantId();
                        // String a = "0db8b4b7-69fd-4843-9904-8408ee1e77d8";
                        // UUID shopId = UUID.fromString(a);
                        return CartItemDto.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .variantId(item.getVariantId())
                                .shopId(shopId)
                                .quantity(item.getQuantity())
                                .price(Double.parseDouble(getPriceFromVariant(variantId))) // convert paise to ₹
                                .build();
                    })
                    .toList();

            // Business logic
            System.out.println("price calculation");
            double totalAmount = itemDtos.stream()
                    .mapToDouble(i -> i.getPrice() * i.getQuantity())
                    .sum();

            double totalDiscount = totalAmount * 0.1; // assume 10% discount
            double serviceCharge = 30.0; // flat service fee
            double deliveryCharge = totalAmount > 500 ? 0 : 50; // free delivery above ₹500
            double gstCharge = totalAmount * 0.18; // 18% GST

            // Final response
            CartResponseDto dto = CartResponseDto.builder()
                    .cartId(cart.getId())
                    .userId(cart.getUserId())
                    .status(cart.getStatus().name())
                    .items(itemDtos)
                    .totalAmount(totalAmount)
                    .totalDiscount(totalDiscount)
                    .serviceCharge(serviceCharge)
                    .deliveryCharge(deliveryCharge)
                    .gstCharge(gstCharge)
                    .build();
            return new ApiResponse<>(true, "Cart fetched", dto, 200);
        } catch (Exception e) {
            System.out.println("Error mesaage" + e.getMessage());
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
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
        UUID variantId = item.getVariantId();
        String priceStr = getPriceFromVariant(variantId);
        BigDecimal base = new BigDecimal(priceStr)
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        BigDecimal disc = computeDiscount(base, coupon.getDiscountType(), coupon.getDiscountValue());
        item.setAppliedCoupon(coupon);
        item.setLineDiscount(disc.toPlainString());

        recompute(cart);

        return cartRepo.save(cart);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getApplicableCoupons(UUID userId) {
        Cart cart = mustGetActiveCart(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return new ApiResponse<>(true, "No items in cart", List.of(), 200);
        }

        BigDecimal subTotal = cart.getItems().stream()
                .map(i -> new BigDecimal(getPriceFromVariant(i.getVariantId()))
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Coupon> activeCoupons = couponRepo.findByActiveTrue();

        // Compute discount for each coupon
        List<CouponResponseDto> applicable = activeCoupons.stream()
                .filter(c -> {
                    try {
                        validateCouponWindow(c);
                        if (c.getScope() == Coupon.Scope.ITEM) {
                            // At least one item matches
                            return cart.getItems().stream().anyMatch(i -> switch (c.getApplicability()) {
                                case PRODUCT -> i.getProductId().equals(c.getProductId());
                                case BRAND, CATEGORY, CART_ALL, CART_TOTAL, ITEM -> true;
                            });
                        } else {
                            // Cart coupon: check min total
                            if (c.getApplicability() == Coupon.Applicability.CART_TOTAL
                                    && c.getMinCartTotal() != null
                                    && subTotal.compareTo(c.getMinCartTotal()) < 0)
                                return false;
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(c -> {
                    BigDecimal discountAmount;
                    if (c.getScope() == Coupon.Scope.ITEM) {
                        // Apply to matching items
                        discountAmount = cart.getItems().stream()
                                .filter(i -> switch (c.getApplicability()) {
                                    case PRODUCT -> i.getProductId().equals(c.getProductId());
                                    case BRAND, CATEGORY, CART_ALL, CART_TOTAL, ITEM -> true;
                                })
                                .map(i -> new BigDecimal(getPriceFromVariant(i.getVariantId()))
                                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                                .map(lineBase -> computeDiscount(lineBase, c.getDiscountType(), c.getDiscountValue()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                    } else {
                        discountAmount = computeDiscount(subTotal, c.getDiscountType(), c.getDiscountValue());
                    }

                    return CouponResponseDto.builder()
                            .id(c.getId())
                            .code(c.getCode())
                            .scope(c.getScope().name())
                            .applicability(c.getApplicability().name())
                            .discountType(c.getDiscountType().name())
                            .discountValue(c.getDiscountValue())
                            .computedDiscount(discountAmount)
                            .build();
                })
                .sorted((a, b) -> b.getComputedDiscount().compareTo(a.getComputedDiscount())) // descending
                .toList();

        return new ApiResponse<>(true, "Applicable coupons", applicable, 200);
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
        if (cart.getItems() == null)
            cart.setItems(new ArrayList<>());
        if (cart.getAppliedCartCoupons() == null)
            cart.setAppliedCartCoupons(new HashSet<>());

        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal itemDisc = BigDecimal.ZERO;

        for (CartItem it : cart.getItems()) {
            UUID variantId = it.getVariantId();
            BigDecimal price = new BigDecimal(getPriceFromVariant(variantId)); // already in paise
            BigDecimal lineBase = price.multiply(BigDecimal.valueOf(it.getQuantity()));
            sub = sub.add(lineBase);

            BigDecimal lineDiscount = new BigDecimal(it.getLineDiscount() == null ? "0" : it.getLineDiscount());
            itemDisc = itemDisc.add(lineDiscount);
        }

        BigDecimal cartBase = sub.subtract(itemDisc);
        BigDecimal cartDisc = BigDecimal.ZERO;

        for (Coupon c : cart.getAppliedCartCoupons()) {
            cartDisc = cartDisc.add(computeDiscount(cartBase, c.getDiscountType(), c.getDiscountValue()));
        }

        BigDecimal taxable = cartBase.subtract(cartDisc).max(BigDecimal.ZERO);

        // ✅ Tax in paise (integer arithmetic)
        // (taxable * 18) / 100 → both are integers, so result is integer paise
        BigDecimal tax = taxable.multiply(BigDecimal.valueOf(18))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                .setScale(0, RoundingMode.UNNECESSARY);

        cart.setSubTotal(sub.stripTrailingZeros().toPlainString());
        cart.setItemLevelDiscount(itemDisc.stripTrailingZeros().toPlainString());
        cart.setCartLevelDiscount(cartDisc.stripTrailingZeros().toPlainString());
        cart.setTax(tax.stripTrailingZeros().toPlainString());
        cart.setGrandTotal(taxable.add(tax).stripTrailingZeros().toPlainString());
    }

    private String getPriceFromVariant(UUID variantId) {
        return variantRepository.findById(variantId)
                .map(v -> v.getPrice())
                .orElse("0");
    }
}
// huyy jggyut nkh jhgjgy guuvu gugyu guyut gg ggyu ygug
// iyhyi7yuyuiyuihuyhuyuhuhuh hyyui7 yuu uy7u8y gyutujt
// y8t76 tutut67u 87y7u8y8 u7t67u7g yuty 6ut67vggtyuty tut6
// uiuiu uu8uujui ujuuj hujuuj hiujuuj huuj huyhui gyyu ghyhijhuk