package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.*;
import com.ProductClientService.ProductClientService.Model.Cart;
import com.ProductClientService.ProductClientService.Model.CartItem;
import com.ProductClientService.ProductClientService.Model.Coupon;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Repository.*;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSellerProjection;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSummaryProjection;
import com.ProductClientService.ProductClientService.Service.BaseService;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class CartService extends BaseService {

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final int        MAX_QTY_PER_ITEM        = 20;
    private static final BigDecimal SERVICE_CHARGE          = BigDecimal.valueOf(30);
    private static final BigDecimal GST_RATE                = BigDecimal.valueOf(18);
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = BigDecimal.valueOf(500);
    private static final BigDecimal DELIVERY_CHARGE_AMOUNT  = BigDecimal.valueOf(50);

    // ── Dependencies ───────────────────────────────────────────────────────────
    private final CartRepository              cartRepo;
    private final CartItemRepository          itemRepo;
    private final CouponRepository            couponRepo;
    private final ProductRepository           productRepository;
    private final ProductVariantRepository    variantRepository;
    private final ProductAttributeRepository  productAttributeRepository;
    private final EventPublisherService       eventPublisher;
    private final com.ProductClientService.ProductClientService.Repository.UserRepojectory userRepo;

    // ── Internal data holders ─────────────────────────────────────────────────

    /**
     * Pre-loaded batch data for a single cart read — avoids N+1 queries.
     * 3 DB queries regardless of how many items the cart contains.
     */
    private record BatchContext(
            Map<UUID, ProductVariant>        variantMap,
            Map<UUID, UUID>                  productToShop,
            Map<UUID, List<ProductAttribute>> imageAttrByProduct
    ) {}

    /** Resolved cart-level coupon data (code, amount, raw string). */
    private record CartCouponInfo(String code, BigDecimal discount, String discountStr) {
        static CartCouponInfo none() { return new CartCouponInfo(null, BigDecimal.ZERO, "0"); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API — cart CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Object> addItem(CartItemRequest req) {
        try {
            Cart cart = getOrCreateActiveCart();
            mergeOrAddItem(cart, req);
            recompute(cart);
            cartRepo.save(cart);
            syncCartItemIds(getUserId(), cart);
            eventPublisher.publishCartAdded(req.getProductId(), req.getVariantId(), getUserId());
            return getCart();
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public ApiResponse<Object> updateQuantity(UUID itemId, int qty) {
        try {
            Cart cart = mustGetActiveCart();
            CartItem item = findCartItem(cart, itemId);
            if (qty <= 0) {
                cart.getItems().remove(item);
                itemRepo.delete(item);
            } else {
                item.setQuantity(qty);
            }
            recompute(cart);
            cartRepo.save(cart);
            syncCartItemIds(getUserId(), cart);
            return getCart();
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public ApiResponse<Object> removeItem(UUID itemId) {
        Cart cart = mustGetActiveCart();
        CartItem item = findCartItem(cart, itemId);
        cart.getItems().remove(item);
        itemRepo.delete(item);
        recompute(cart);
        cartRepo.save(cart);
        syncCartItemIds(getUserId(), cart);
        return getCart();
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getCart() {
        return getCartByUserId(getUserId());
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getCartByUserId(UUID userId) {
        try {
            Cart cart = cartRepo.findByUserIdAndStatus(userId, Cart.Status.ACTIVE).orElse(null);
            List<CartItem> cartItems = cart != null ? cart.getItems() : List.of();

            if (cart == null || cartItems.isEmpty()) {
                return new ApiResponse<>(true, "Cart fetched", emptyCartResponse(userId, cart), 200);
            }

            List<CartValidationIssue> issues      = new ArrayList<>();
            BatchContext              ctx          = loadBatchContext(cartItems);
            CartCouponInfo            couponInfo   = resolveCartCoupon(cart, issues);
            List<CartItemDto>         itemDtos     = buildValidatedItems(cartItems, ctx, issues);
            BigDecimal                totalNet     = netAfterItemDiscounts(itemDtos);
            Map<UUID, List<CartItemDto>> byShop    = groupByShop(itemDtos);
            List<SubOrderDto>         subOrders    = buildSubOrders(byShop, couponInfo, totalNet);

            return new ApiResponse<>(true, "Cart fetched",
                    assembleCartResponse(cart, userId, itemDtos, subOrders, couponInfo, issues), 200);

        } catch (Exception e) {
            log.error("Error building cart for userId={}", userId, e);
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    @Transactional
    public Cart clearCart() {
        Cart cart = mustGetActiveCart();
        cart.getItems().clear();
        cart.setAppliedCartCoupon(null);
        cart.setItemLevelDiscount("0");
        cart.setCartLevelDiscount("0");
        recompute(cart);
        return cartRepo.save(cart);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API — coupons
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Object> applyItemCoupon(ApplyCouponRequest req) {
        if (req.getItemId() == null)
            throw new IllegalArgumentException("itemId required for item coupon");

        Cart cart     = mustGetActiveCart();
        CartItem item = findCartItem(cart, req.getItemId());
        Coupon coupon = fetchActiveCoupon(req.getCode());
        validateCouponWindow(coupon);

        if (coupon.getScope() != Coupon.Scope.ITEM)
            throw new RuntimeException("Coupon is not item-scope");

        validateCouponApplicability(item, coupon);
        applyDiscountToItem(item, coupon);
        recompute(cart);
        cartRepo.save(cart);
        return getCart();
    }

    @Transactional
    public ApiResponse<Object> removeItemCoupon(UUID itemId) {
        Cart cart     = mustGetActiveCart();
        CartItem item = findCartItem(cart, itemId);
        item.setAppliedCoupon(null);
        item.setLineDiscount("0");
        recompute(cart);
        cartRepo.save(cart);
        return getCart();
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getApplicableCoupons() {
        Cart cart = mustGetActiveCart();
        if (cart.getItems() == null || cart.getItems().isEmpty())
            return new ApiResponse<>(true, "No items in cart", List.of(), 200);

        String subTotal      = cart.getSubTotal();
        long   subTotalValue = Long.parseLong(subTotal);

        List<CouponResponseDto.BestCoupon> bestCoupons = fetchEligibleCoupons(subTotal).stream()
                .sorted(Comparator.comparingLong((Coupon c) -> calculateDiscount(c, subTotalValue)).reversed())
                .map(c -> toBestCoupon(c, subTotalValue))
                .toList();

        List<CouponResponseDto.MoreCoupon> moreCoupons = buildMoreCoupons(subTotal);

        return new ApiResponse<>(true, "Applicable coupons",
                Map.of("bestCoupons", bestCoupons, "moreCoupons", moreCoupons), 200);
    }

    @Transactional
    public ApiResponse<Object> applyCartCoupon(String code) {
        Cart   cart   = mustGetActiveCart();
        Coupon coupon = fetchActiveCoupon(code);
        validateCouponWindow(coupon);

        if (coupon.getScope() != Coupon.Scope.CART)
            throw new IllegalArgumentException("Coupon is not cart-scope");

        validateCartMinimum(cart, coupon);
        BigDecimal net = cartNetSubTotal(cart);
        cart.setAppliedCartCoupon(coupon);
        cart.setCartLevelDiscount(computeDiscount(net, coupon.getDiscountType(), coupon.getDiscountValue()).toString());
        recompute(cart);
        cartRepo.save(cart);
        return getCart();
    }

    @Transactional
    public ApiResponse<Object> removeCartCoupon(String code) {
        Cart cart = mustGetActiveCart();
        couponRepo.findByCodeIgnoreCaseAndActiveTrue(code).ifPresent(c -> cart.setAppliedCartCoupon(null));
        recompute(cart);
        cartRepo.save(cart);
        return getCart();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CART BUILD — step-by-step private helpers (called only by getCartByUserId)
    // ═══════════════════════════════════════════════════════════════════════════

    private CartResponseDto emptyCartResponse(UUID userId, Cart cart) {
        return CartResponseDto.builder()
                .userId(userId)
                .status(cart != null ? cart.getStatus().name() : Cart.Status.ACTIVE.name())
                .items(List.of()).subOrders(List.of()).validationIssues(List.of())
                .totalAmount(0).totalDiscount(0).serviceCharge(r2(SERVICE_CHARGE))
                .deliveryCharge(0).gstCharge(0).grandTotal(r2(SERVICE_CHARGE))
                .cartLineDiscount("0")
                .build();
    }

    /** Loads variants, seller IDs, and image attributes in 3 DB queries. */
    private BatchContext loadBatchContext(List<CartItem> cartItems) {
        Set<UUID> variantIds = cartItems.stream().map(CartItem::getVariantId).collect(Collectors.toSet());
        Set<UUID> productIds = cartItems.stream().map(CartItem::getProductId).collect(Collectors.toSet());

        Map<UUID, ProductVariant> variantMap = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Map<UUID, UUID> productToShop = productRepository.findSellerIdsByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        ProductSellerProjection::getProductId,
                        ProductSellerProjection::getSellerId));

        Map<UUID, List<ProductAttribute>> imageAttrByProduct = productAttributeRepository
                .findImageAttributesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(pa -> pa.getProduct().getId()));

        return new BatchContext(variantMap, productToShop, imageAttrByProduct);
    }

    /** Validates the cart-level coupon; adds a CART_COUPON_EXPIRED issue if stale. */
    private CartCouponInfo resolveCartCoupon(Cart cart, List<CartValidationIssue> issues) {
        Coupon coupon = cart.getAppliedCartCoupon();
        if (coupon == null) return CartCouponInfo.none();

        if (!isCouponLive(coupon)) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.CART_COUPON_EXPIRED, null, null,
                    "Cart coupon '" + coupon.getCode() + "' has expired and was excluded from totals"));
            return CartCouponInfo.none();
        }

        String discStr = cart.getCartLevelDiscount() != null ? cart.getCartLevelDiscount() : "0";
        return new CartCouponInfo(coupon.getCode(), parseDecimal(discStr), discStr);
    }

    /** Converts each CartItem to a CartItemDto, skipping unavailable variants. */
    private List<CartItemDto> buildValidatedItems(List<CartItem> cartItems,
                                                  BatchContext ctx,
                                                  List<CartValidationIssue> issues) {
        List<CartItemDto> result = new ArrayList<>();
        for (CartItem item : cartItems) {
            buildCartItemDto(item, ctx, issues).ifPresent(result::add);
        }
        return result;
    }

    /** Builds one CartItemDto. Returns empty if the variant no longer exists. */
    private Optional<CartItemDto> buildCartItemDto(CartItem item,
                                                   BatchContext ctx,
                                                   List<CartValidationIssue> issues) {
        ProductVariant variant = ctx.variantMap().get(item.getVariantId());
        if (variant == null) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.ITEM_UNAVAILABLE,
                    item.getId(), item.getProductId(),
                    "Variant " + item.getVariantId() + " is no longer available"));
            return Optional.empty();
        }

        boolean available   = checkStock(item, variant, issues);
        String  lineDiscount = resolveEffectiveLineDiscount(item, issues);
        String  couponCode   = isCouponLive(item.getAppliedCoupon()) ? item.getAppliedCoupon().getCode() : null;
        String  image        = resolveImageUrl(
                ctx.imageAttrByProduct().getOrDefault(item.getProductId(), List.of()), variant.getSku());

        ProductSummaryProjection summary = productRepository.getProductNameAndDescription(item.getProductId());

        return Optional.of(CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .shopId(ctx.productToShop().get(item.getProductId()))
                .quantity(item.getQuantity())
                .price(parseDecimal(variant.getPrice()).doubleValue())
                .name(summary != null ? summary.getName() : "Unknown")
                .description(summary != null ? summary.getDescription() : "")
                .image(image)
                .appliedCoupon(couponCode)
                .discountLineAmount(lineDiscount)
                .stockAvailable(variant.getStock())
                .isAvailable(available)
                .build());
    }

    /** Validates stock levels; returns true if the item can proceed to checkout. */
    private boolean checkStock(CartItem item, ProductVariant variant, List<CartValidationIssue> issues) {
        int qty   = item.getQuantity();
        int stock = variant.getStock();

        if (qty > MAX_QTY_PER_ITEM) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.INSUFFICIENT_STOCK,
                    item.getId(), item.getProductId(),
                    "Quantity " + qty + " exceeds maximum allowed " + MAX_QTY_PER_ITEM));
        }
        if (stock == 0) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.OUT_OF_STOCK,
                    item.getId(), item.getProductId(), "Product is out of stock"));
            return false;
        }
        if (stock < qty) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.INSUFFICIENT_STOCK,
                    item.getId(), item.getProductId(),
                    "Only " + stock + " unit(s) available, cart has " + qty));
            return false;
        }
        return true;
    }

    /** Returns the effective line discount, zeroing it if the item coupon has expired. */
    private String resolveEffectiveLineDiscount(CartItem item, List<CartValidationIssue> issues) {
        Coupon coupon = item.getAppliedCoupon();
        if (coupon == null) return item.getLineDiscount() != null ? item.getLineDiscount() : "0";

        if (!isCouponLive(coupon)) {
            issues.add(new CartValidationIssue(CartValidationIssue.Type.COUPON_EXPIRED,
                    item.getId(), item.getProductId(),
                    "Item coupon '" + coupon.getCode() + "' has expired"));
            return "0";
        }
        return item.getLineDiscount() != null ? item.getLineDiscount() : "0";
    }

    private Map<UUID, List<CartItemDto>> groupByShop(List<CartItemDto> items) {
        return items.stream().collect(Collectors.groupingBy(CartItemDto::getShopId));
    }

    /** Total (subTotal − itemDiscounts) across all items — the base for coupon splitting. */
    private BigDecimal netAfterItemDiscounts(List<CartItemDto> items) {
        BigDecimal sub  = items.stream().map(i -> bd(i.getPrice()).multiply(bd(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disc = items.stream().map(i -> parseDecimal(i.getDiscountLineAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sub.subtract(disc);
    }

    private List<SubOrderDto> buildSubOrders(Map<UUID, List<CartItemDto>> byShop,
                                             CartCouponInfo couponInfo,
                                             BigDecimal totalNet) {
        return byShop.entrySet().stream()
                .map(e -> buildSubOrder(e.getKey(), e.getValue(), couponInfo, totalNet))
                .collect(Collectors.toList());
    }

    /** Builds one shop's sub-order with proportional cart-coupon allocation. */
    private SubOrderDto buildSubOrder(UUID shopId, List<CartItemDto> shopItems,
                                      CartCouponInfo couponInfo, BigDecimal totalNet) {
        BigDecimal shopSub      = shopItems.stream().map(i -> bd(i.getPrice()).multiply(bd(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shopItemDisc = shopItems.stream().map(i -> parseDecimal(i.getDiscountLineAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shopNet      = shopSub.subtract(shopItemDisc);
        BigDecimal shopCartDisc = proportionalDiscount(shopNet, totalNet, couponInfo.discount());
        BigDecimal taxable      = shopNet.subtract(shopCartDisc).max(BigDecimal.ZERO);
        BigDecimal gst          = taxable.multiply(GST_RATE).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal delivery     = taxable.compareTo(FREE_DELIVERY_THRESHOLD) > 0 ? BigDecimal.ZERO : DELIVERY_CHARGE_AMOUNT;

        return SubOrderDto.builder()
                .shopId(shopId).items(shopItems)
                .subTotal(r2(shopSub)).itemLevelDiscount(r2(shopItemDisc))
                .proportionalCartDiscount(r2(shopCartDisc))
                .taxableAmount(r2(taxable)).gstCharge(r2(gst)).deliveryCharge(r2(delivery))
                .subOrderTotal(r2(taxable.add(gst).add(delivery)))
                .build();
    }

    /** discount × (shopNet / totalNet), or zero if either operand is zero. */
    private BigDecimal proportionalDiscount(BigDecimal shopNet, BigDecimal totalNet, BigDecimal cartDiscount) {
        if (totalNet.compareTo(BigDecimal.ZERO) <= 0 || cartDiscount.compareTo(BigDecimal.ZERO) <= 0)
            return BigDecimal.ZERO;
        return cartDiscount.multiply(shopNet).divide(totalNet, 2, RoundingMode.HALF_UP);
    }

    /** Assembles the final CartResponseDto from pre-computed parts. */
    private CartResponseDto assembleCartResponse(Cart cart, UUID userId,
                                                 List<CartItemDto> items,
                                                 List<SubOrderDto> subOrders,
                                                 CartCouponInfo couponInfo,
                                                 List<CartValidationIssue> issues) {
        BigDecimal totalSub      = items.stream().map(i -> bd(i.getPrice()).multiply(bd(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalItemDisc = items.stream().map(i -> parseDecimal(i.getDiscountLineAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDelivery = subOrders.stream().map(s -> bd(s.getDeliveryCharge())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGst      = subOrders.stream().map(s -> bd(s.getGstCharge())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal    = subOrders.stream().map(s -> bd(s.getSubOrderTotal())).reduce(BigDecimal.ZERO, BigDecimal::add).add(SERVICE_CHARGE);

        return CartResponseDto.builder()
                .cartId(cart.getId()).userId(userId).status(cart.getStatus().name())
                .items(items).subOrders(subOrders).validationIssues(issues)
                .totalAmount(r2(totalSub))
                .totalDiscount(r2(totalItemDisc.add(couponInfo.discount())))
                .serviceCharge(r2(SERVICE_CHARGE))
                .deliveryCharge(r2(totalDelivery))
                .gstCharge(r2(totalGst))
                .grandTotal(r2(grandTotal))
                .cartCoupon(couponInfo.code())
                .cartLineDiscount(couponInfo.discountStr())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUPON DISPLAY — private helpers (called only by getApplicableCoupons)
    // ═══════════════════════════════════════════════════════════════════════════

    private List<Coupon> fetchEligibleCoupons(String subTotal) {
        return couponRepo.findByActiveTrueAndApplicabilityAndMinCartTotalLessThanEqual(
                        Coupon.Applicability.CART_TOTAL, subTotal).stream()
                .filter(c -> c.getEndsAt() != null && c.getEndsAt().isAfter(ZonedDateTime.now()))
                .toList();
    }

    private CouponResponseDto.BestCoupon toBestCoupon(Coupon c, long subTotalValue) {
        long discountAmount = calculateDiscount(c, subTotalValue);
        return CouponResponseDto.BestCoupon.builder()
                .id(c.getId()).code(c.getCode())
                .leftParagraph(formatLeftParagraph(c))
                .saveDescription("Save ₹ " + discountAmount + " on this Order")
                .description(formatCouponDescription(c, discountAmount))
                .build();
    }

    public List<CouponResponseDto.MoreCoupon> buildMoreCoupons(String subTotal) {
        return couponRepo.findByActiveTrueAndApplicabilityAndMinCartTotalGreaterThan(
                        Coupon.Applicability.CART_TOTAL, subTotal).stream()
                .sorted(Comparator.comparingLong((Coupon c) -> calculateDiscount(c, Long.parseLong(subTotal))))
                .map(c -> toMoreCoupon(c, subTotal))
                .collect(Collectors.toList());
    }

    private CouponResponseDto.MoreCoupon toMoreCoupon(Coupon c, String subTotal) {
        long discountAmount = calculateDiscount(c, Long.parseLong(c.getMinCartTotal()));
        long gap            = Long.parseLong(c.getMinCartTotal()) - Long.parseLong(subTotal);
        return CouponResponseDto.MoreCoupon.builder()
                .id(c.getId()).code(c.getCode())
                .leftParagraph(formatLeftParagraph(c))
                .addMoreDescription("Add More ₹ " + gap + " to avail this Offer")
                .subDescription("Get " + c.getDiscountValue() + (c.getDiscountType() == Coupon.DiscountType.PERCENT ? " % Off" : " ₹ Flat"))
                .description(formatCouponDescription(c, discountAmount))
                .build();
    }

    private String formatLeftParagraph(Coupon c) {
        return c.getDiscountType() == Coupon.DiscountType.PERCENT
                ? c.getDiscountValue() + " % Off"
                : "₹" + c.getDiscountValue() + " Off";
    }

    private String formatCouponDescription(Coupon c, long discountAmount) {
        return String.format("Use Code %s & get %s %s off on Order Above ₹ %s. Maximum discount %s.",
                c.getCode(), c.getDiscountValue(),
                c.getDiscountType() == Coupon.DiscountType.PERCENT ? "%" : "₹",
                c.getMinCartTotal(), discountAmount);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED HELPERS — cart, coupon, item utilities
    // ═══════════════════════════════════════════════════════════════════════════

    private Cart getOrCreateActiveCart() {
        return cartRepo.findByUserIdAndStatus(getUserId(), Cart.Status.ACTIVE)
                .orElseGet(() -> cartRepo.save(Cart.builder()
                        .userId(getUserId()).status(Cart.Status.ACTIVE).build()));
    }

    private Cart mustGetActiveCart() {
        return cartRepo.findByUserIdAndStatus(getUserId(), Cart.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active cart for user"));
    }

    private CartItem findCartItem(Cart cart, UUID itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart"));
    }

    private void mergeOrAddItem(Cart cart, CartItemRequest req) {
        if (cart.getItems() == null) cart.setItems(new ArrayList<>());
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId())
                        && Objects.equals(i.getVariantId(), req.getVariantId()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + Math.max(1, req.getQuantity()));
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart).productId(req.getProductId()).variantId(req.getVariantId())
                    .quantity(Math.max(1, req.getQuantity())).metadata(req.getMetadata()).lineDiscount("0")
                    .build());
        }
    }

    private Coupon fetchActiveCoupon(String code) {
        return couponRepo.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new RuntimeException("Invalid coupon"));
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

    private boolean isCouponLive(Coupon coupon) {
        if (coupon == null || !Boolean.TRUE.equals(coupon.getActive())) return false;
        return coupon.getEndsAt() == null || !ZonedDateTime.now().isAfter(coupon.getEndsAt());
    }

    private void validateCouponApplicability(CartItem item, Coupon coupon) {
        switch (coupon.getApplicability()) {
            case PRODUCT -> {
                if (!item.getProductId().equals(coupon.getProductId()))
                    throw new IllegalArgumentException("Coupon not applicable on this product");
            }
            case BRAND, CATEGORY -> { /* extend with brand/category lookups when needed */ }
            default -> { /* CART_TOTAL / ITEM scope — no product-level restriction */ }
        }
    }

    private void applyDiscountToItem(CartItem item, Coupon coupon) {
        BigDecimal base = new BigDecimal(getPriceFromVariant(item.getVariantId()))
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal disc = computeDiscount(base, coupon.getDiscountType(), coupon.getDiscountValue());
        item.setAppliedCoupon(coupon);
        item.setLineDiscount(disc.toPlainString());
    }

    private void validateCartMinimum(Cart cart, Coupon coupon) {
        if (coupon.getApplicability() != Coupon.Applicability.CART_TOTAL || coupon.getMinCartTotal() == null)
            return;
        if (cartNetSubTotal(cart).compareTo(new BigDecimal(coupon.getMinCartTotal())) < 0)
            throw new IllegalArgumentException("Cart total below minimum for this coupon");
    }

    private BigDecimal cartNetSubTotal(Cart cart) {
        return new BigDecimal(cart.getSubTotal()).subtract(new BigDecimal(cart.getItemLevelDiscount()));
    }

    /** Recomputes and persists all financial fields on the Cart entity (for write operations). */
    private void recompute(Cart cart) {
        if (cart.getItems() == null) cart.setItems(new ArrayList<>());

        BigDecimal sub      = BigDecimal.ZERO;
        BigDecimal itemDisc = BigDecimal.ZERO;

        for (CartItem it : cart.getItems()) {
            BigDecimal price = new BigDecimal(getPriceFromVariant(it.getVariantId()));
            sub      = sub.add(price.multiply(BigDecimal.valueOf(it.getQuantity())));
            itemDisc = itemDisc.add(parseDecimal(it.getLineDiscount()));
        }

        BigDecimal cartBase = sub.subtract(itemDisc);
        Coupon     c        = cart.getAppliedCartCoupon();
        BigDecimal cartDisc = c != null
                ? computeDiscount(cartBase, c.getDiscountType(), c.getDiscountValue())
                : BigDecimal.ZERO;
        BigDecimal taxable  = cartBase.subtract(cartDisc).max(BigDecimal.ZERO);
        BigDecimal tax      = taxable.multiply(BigDecimal.valueOf(18))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

        cart.setSubTotal(sub.stripTrailingZeros().toPlainString());
        cart.setItemLevelDiscount(itemDisc.stripTrailingZeros().toPlainString());
        cart.setCartLevelDiscount(cartDisc.stripTrailingZeros().toPlainString());
        cart.setTax(tax.stripTrailingZeros().toPlainString());
        cart.setGrandTotal(taxable.add(tax).stripTrailingZeros().toPlainString());
    }

    private BigDecimal computeDiscount(BigDecimal base, Coupon.DiscountType type, String value) {
        if (value == null || base.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        try {
            BigDecimal v = new BigDecimal(value);
            return switch (type) {
                case FLAT    -> v.min(base);
                case PERCENT -> base.multiply(v).divide(BigDecimal.valueOf(100));
            };
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String resolveImageUrl(List<ProductAttribute> attrs, String sku) {
        if (attrs == null || attrs.isEmpty()) return null;
        String[] parts = sku != null ? sku.split("-") : new String[0];
        String   key   = parts.length > 1 ? parts[1] : (parts.length > 0 ? parts[0] : "");
        return attrs.stream()
                .filter(pa -> pa.getValue().equalsIgnoreCase(key)
                        && pa.getImages() != null && !pa.getImages().isEmpty())
                .map(pa -> pa.getImages().get(0))
                .findFirst()
                .orElseGet(() -> attrs.stream()
                        .filter(pa -> pa.getImages() != null && !pa.getImages().isEmpty())
                        .map(pa -> pa.getImages().get(0)).findFirst().orElse(null));
    }

    private String getPriceFromVariant(UUID variantId) {
        return variantRepository.findById(variantId).map(ProductVariant::getPrice).orElse("0");
    }

    public static long calculateDiscount(Coupon coupon, long cartAmount) {
        if (coupon.getDiscountType() == Coupon.DiscountType.FLAT)
            return Long.parseLong(coupon.getDiscountValue());
        double percent = Double.parseDouble(coupon.getDiscountValue());
        if (coupon.getUptoAmount() != null)
            return Math.round(Long.parseLong(coupon.getUptoAmount()) * percent / 100.0);
        return Math.round(cartAmount * percent / 100.0);
    }

    // ── User jsonb sync ───────────────────────────────────────────────────────

    private void syncCartItemIds(UUID userId, Cart cart) {
        try {
            userRepo.findById(userId).ifPresent(user -> {
                Set<UUID> ids = cart.getItems() == null ? new HashSet<>() :
                        cart.getItems().stream().map(CartItem::getId).collect(Collectors.toSet());
                user.setCartItemIds(ids);
                userRepo.save(user);
            });
        } catch (Exception e) {
            log.warn("Failed to sync cartItemIds for userId={}: {}", userId, e.getMessage());
        }
    }

    // ── Math utilities ─────────────────────────────────────────────────────────

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private static BigDecimal bd(double value) { return BigDecimal.valueOf(value); }
    private static BigDecimal bd(int value)    { return BigDecimal.valueOf(value); }

    private static double r2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
