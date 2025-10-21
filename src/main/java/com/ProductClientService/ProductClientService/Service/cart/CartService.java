package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemDto;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartResponseDto;
import com.ProductClientService.ProductClientService.DTO.Cart.CouponResponseDto;
import com.ProductClientService.ProductClientService.Model.Cart;
import com.ProductClientService.ProductClientService.Model.CartItem;
import com.ProductClientService.ProductClientService.Model.Coupon;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Repository.*;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSummaryProjection;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private final ProductAttributeRepository productAttributeRepository;

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
            return getCart(userId);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
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
            cartRepo.save(cart);
            return getCart(userId);
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
        cartRepo.save(cart);
        return getCart(userId);
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
                        ProductDetailsDto productDetails = getProductDetails(item);
                        // String a = "0db8b4b7-69fd-4843-9904-8408ee1e77d8";
                        // UUID shopId = UUID.fromString(a);
                        return CartItemDto.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .appliedCoupon(Optional.ofNullable(item.getAppliedCoupon())
                                .map(Coupon::getCode)
                                .orElse(null))
                                .discountLineAmount(item.getLineDiscount())
                                .variantId(item.getVariantId())
                                .shopId(shopId)
                                .quantity(item.getQuantity())
                                .price(Double.parseDouble(getPriceFromVariant(variantId))) // convert paise to ₹
                                .name(productDetails.getName())
                                .description(productDetails.getDescription())
                                .image(productDetails.getImageUrl())
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
            double grandTotal=totalAmount-totalDiscount+serviceCharge+deliveryCharge+gstCharge;
            CartResponseDto dto = CartResponseDto.builder()
                    .cartId(cart.getId())
                    .userId(cart.getUserId())
                    .status(cart.getStatus().name())
                    .items(itemDtos)
                    .totalAmount(totalAmount)
                    .totalDiscount(totalDiscount)
                    .serviceCharge(serviceCharge)
                    .deliveryCharge(deliveryCharge)
                    .CartLineDiscount(cart.getCartLevelDiscount())
                    .cartCoupon(cart.getAppliedCartCoupon().getCode())
                    .gstCharge(gstCharge)
                    .grandTotal(grandTotal)
                    .build();
            return new ApiResponse<>(true, "Cart fetched", dto, 200);
        } catch (Exception e) {
            System.out.println("Error mesaage" + e.getMessage());
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    private ProductDetailsDto getProductDetails(CartItem item) {
        UUID variantId = item.getVariantId();
        UUID productId = item.getProductId();

        // 1. Fetch name and description (projection)
        ProductSummaryProjection productSummary = productRepository.getProductNameAndDescription(productId);

        if (productSummary == null) {
            throw new RuntimeException("Product not found for ID: " + productId);
        }

        // 2. Fetch variant
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found for ID: " + variantId));

        // 3. Determine image attribute from SKU (safe)
        String[] skuParts = variant.getSku().split("-");
        String imageAttribute = skuParts.length > 1 ? skuParts[1] : skuParts[0];

        // 4. Fetch all image attributes for this product
        List<ProductAttribute> attributes = productAttributeRepository.findImageAttributesByProductId(productId);

        // 5. Find matching image URL
        String imageUrl = null;
        for (ProductAttribute pa : attributes) {
            if (pa.getValue().equalsIgnoreCase(imageAttribute) && pa.getImages() != null && !pa.getImages().isEmpty()) {
                imageUrl = pa.getImages().get(0);
                break;
            }
        }

        // 6. Fallback if no image found
        if (imageUrl == null && !attributes.isEmpty() && !attributes.get(0).getImages().isEmpty()) {
            imageUrl = attributes.get(0).getImages().get(0);
        }

        return ProductDetailsDto.builder()
                .name(productSummary.getName())
                .description(productSummary.getDescription())
                .imageUrl(imageUrl)
                .build();
    }

    @Transactional
    public Cart clearCart(UUID userId) {
        Cart cart = mustGetActiveCart(userId);
        cart.getItems().clear();
        cart.setAppliedCartCoupon(null);
        cart.setItemLevelDiscount("0");
        cart.setCartLevelDiscount("0");
        recompute(cart);
        return cartRepo.save(cart);
    }

    // ---------- Coupons ----------
    @Transactional
    public ApiResponse<Object> applyItemCoupon(UUID userId, ApplyCouponRequest req) {
        if (req.getItemId() == null)
            throw new IllegalArgumentException("itemId required for item coupon");
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(req.getItemId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found"));

        Coupon coupon = couponRepo.findByCodeIgnoreCaseAndActiveTrue(req.getCode())
                .orElseThrow(() -> new RuntimeException("Invalid coupon"));
        validateCouponWindow(coupon);

        if (coupon.getScope() != Coupon.Scope.ITEM)
            throw new RuntimeException("Coupon is not item-scope");

        // Validate applicability (here you can query product->brand/category if needed)
        // For demo: only PRODUCT applicability strictly checks productId
        switch (coupon.getApplicability()) {
            case PRODUCT -> {
                if (!item.getProductId().equals(coupon.getProductId()))
                    throw new IllegalArgumentException("Coupon not applicable on this product");
            }
            case BRAND, CATEGORY -> {
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
        cartRepo.save(cart);
        return getCart(userId);
    }

    @Transactional
    public ApiResponse<Object> removeItemCoupon(UUID userId, UUID itemId) {
        Cart cart = mustGetActiveCart(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        item.setAppliedCoupon(null);
        item.setLineDiscount("0");
        recompute(cart);
        cartRepo.save(cart);
        return getCart(userId);
    }
    @Transactional(readOnly = true)
    public ApiResponse<Object> getApplicableCoupons(UUID userId) {
        Cart cart = mustGetActiveCart(userId);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return new ApiResponse<>(true, "No items in cart", List.of(), 200);
        }

        String subTotal = cart.getSubTotal();
        long subTotalValue = Long.parseLong(subTotal); // assuming paise
        System.out.println("Cart subtotal: " + subTotalValue);

        // Fetch eligible coupons
        System.out.println("subTotal before "+subTotal);
        List<Coupon> eligibleCoupons = couponRepo.findByActiveTrueAndApplicabilityAndMinCartTotalLessThanEqual(
                Coupon.Applicability.CART_TOTAL,
                subTotal);

        // Sort by discount descending
        List<Coupon> sortedCoupons = eligibleCoupons.stream()
                .sorted(Comparator.comparingLong((Coupon c) -> calculateDiscount(c, subTotalValue))
                        .reversed())
                .toList();

        // Map to response DTO
        List<CouponResponseDto.BestCoupon> bestCoupons = sortedCoupons.stream()
                .map(c -> {
                    long discountAmount = calculateDiscount(c, subTotalValue);

                    String leftParagraph;
                    if (c.getDiscountType() == Coupon.DiscountType.PERCENT) {
                        leftParagraph = c.getDiscountValue() + " % Off";
                    } else { // FLAT
                        leftParagraph = "₹" + c.getDiscountValue() + " Off";
                    }

                    String saveDescription = "Save ₹ " + discountAmount + " on this Order";

                    String description = String.format(
                            "Use Code %s & get %s %s off on Order Above ₹ %s. Maximum discount %s.",
                            c.getCode(),
                            c.getDiscountValue(),
                            c.getDiscountType() == Coupon.DiscountType.PERCENT ? "%" : "₹",
                            c.getMinCartTotal(),
                            discountAmount);

                    return CouponResponseDto.BestCoupon.builder()
                            .id(c.getId())
                            .code(c.getCode())
                            .leftParagraph(leftParagraph)
                            .saveDescription(saveDescription)
                            .description(description)
                            .build();
                })
                .toList();
        List<CouponResponseDto.MoreCoupon> moreCoupons=MoreCoupons(subTotal);
        HashMap<String,Object>results= new HashMap<>();
        results.put("bestCoupons",bestCoupons);
        results.put("moreCoupons",moreCoupons);
        return new ApiResponse<>(true, "Applicable coupons", results, 200);
    }

    public List<CouponResponseDto.MoreCoupon> MoreCoupons(String subTotal) {
        List<Coupon> moreCoupons = couponRepo.findByActiveTrueAndApplicabilityAndMinCartTotalGreaterThan(
                Coupon.Applicability.CART_TOTAL,
                subTotal);
        moreCoupons = moreCoupons.stream()
                .sorted(Comparator.comparingLong((Coupon c) -> calculateDiscount(c, Long.parseLong(subTotal))))
                .toList();
        return moreCoupons.stream()
                .map(c -> {
                    long discountAmount = calculateDiscount(c, Long.parseLong(c.getMinCartTotal()));

                    String leftParagraph = c.getDiscountType() == Coupon.DiscountType.PERCENT
                            ? c.getDiscountValue() + " % Off"
                            : "₹" + c.getDiscountValue() + " Off";

                    String addMoreDescription = "Add More ₹ " +
                            (Long.parseLong(c.getMinCartTotal()) - Long.parseLong(subTotal)) +
                            " to avail this Offer";

                    String subDescription = "Get " + c.getDiscountValue() +
                            (c.getDiscountType() == Coupon.DiscountType.PERCENT ? " % Off" : " ₹ Flat");

                    String description = String.format(
                            "Use Code %s & get %s %s off on Order Above ₹ %s. Maximum discount %s.",
                            c.getCode(),
                            c.getDiscountValue(),
                            c.getDiscountType() == Coupon.DiscountType.PERCENT ? "%" : "₹",
                            c.getMinCartTotal(),
                            discountAmount);

                    return CouponResponseDto.MoreCoupon.builder()
                            .id(c.getId())
                            .code(c.getCode())
                            .addMoreDescription(addMoreDescription)
                            .subDescription(subDescription)
                            .leftParagraph(leftParagraph)
                            .description(description)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse<Object> applyCartCoupon(UUID userId, String code) {
        Cart cart = mustGetActiveCart(userId);
        Coupon coupon = couponRepo.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new RuntimeException("Invalid coupon"));
        validateCouponWindow(coupon);
        if (coupon.getScope() != Coupon.Scope.CART)
            throw new IllegalArgumentException("Coupon is not cart-scope");

        // validate min cart total if needed
        BigDecimal currentSubTotal = new BigDecimal(cart.getSubTotal())
                .subtract(new BigDecimal(cart.getItemLevelDiscount()));
        if (coupon.getApplicability() == Coupon.Applicability.CART_TOTAL
                && coupon.getMinCartTotal() != null
                && currentSubTotal.compareTo(new BigDecimal(coupon.getMinCartTotal())) < 0) {
            throw new IllegalArgumentException("Cart total below minimum for this coupon");
        }
        cart.setAppliedCartCoupon(coupon);
        cart.setCartLevelDiscount(computeDiscount(currentSubTotal,coupon.getDiscountType(),coupon.getDiscountValue()).toString());
        recompute(cart);
        cartRepo.save(cart);
        return getCart(userId);
    }

    @Transactional
    public ApiResponse<Object> removeCartCoupon(UUID userId, String code) {
        Cart cart = mustGetActiveCart(userId);
        couponRepo.findByCodeIgnoreCaseAndActiveTrue(code).ifPresent(c -> cart.setAppliedCartCoupon(null));
        recompute(cart);
        cartRepo.save(cart);
        return getCart(userId);
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

    private BigDecimal computeDiscount(BigDecimal base, Coupon.DiscountType type, String value) {
        if (value == null || base.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;

        BigDecimal valueDecimal;
        try {
            valueDecimal = new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO; // or handle invalid input gracefully
        }

        return switch (type) {
            case FLAT -> valueDecimal.min(base); // ensure discount doesn't exceed base
            case PERCENT -> base.multiply(valueDecimal).divide(BigDecimal.valueOf(100));
        };
    }

    private void recompute(Cart cart) {
        if (cart.getItems() == null)
            cart.setItems(new ArrayList<>());

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
        Coupon c = cart.getAppliedCartCoupon();
        if (c != null) {
            cartDisc = computeDiscount(cartBase, c.getDiscountType(), c.getDiscountValue());
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

    public static long calculateDiscount(Coupon coupon, long cartAmount) {
        if (coupon.getDiscountType() == Coupon.DiscountType.FLAT) {
            return Long.parseLong(coupon.getDiscountValue()); // stored in paise
        } else { // PERCENT
            double percent = Double.parseDouble(coupon.getDiscountValue());
            if (coupon.getUptoAmount() != null)
                return Math.round(Long.parseLong(coupon.getUptoAmount()) * percent / 100.0);
            return Math.round(cartAmount * percent / 100.0);
        }
    }
}



