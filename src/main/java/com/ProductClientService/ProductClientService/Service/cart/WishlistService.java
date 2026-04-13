package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.SharedWishlist;
import com.ProductClientService.ProductClientService.Model.Wishlist;
import com.ProductClientService.ProductClientService.Model.WishlistItem;
import com.ProductClientService.ProductClientService.Repository.*;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.DTO.wishlist.PriceDropItemDto;

import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepo;
    private final WishlistItemRepository itemRepo;
    private final CartService cartService;
    private final ProductVariantRepository variantRepo;
    private final ProductRepository productRepo;
    private final SharedWishlistRepository sharedWishlistRepo;
    private final EventPublisherService eventPublisher;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final SecureRandom RNG = new SecureRandom();
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Transactional
    public ApiResponse<Object> add(UUID userId, UUID productId, UUID variantId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Wishlist newWl = Wishlist.builder().userId(userId).build();
                    newWl.setItems(new ArrayList<>()); // ensure items list is initialized
                    return wishlistRepo.save(newWl);
                });

        if (wl.getItems() == null)
            wl.setItems(new ArrayList<>()); // safety check

        boolean exists = wl.getItems().stream()
                .anyMatch(i -> i.getProductId().equals(productId) && Objects.equals(i.getVariantId(), variantId));

        if (!exists) {
            wl.getItems().add(WishlistItem.builder()
                    .wishlist(wl).productId(productId).variantId(variantId).build());
            eventPublisher.publishWishlistAdd(productId, userId);
        }

        wishlistRepo.save(wl);
        return new ApiResponse<>(true, "Item added to wishlist", wl, 200);
    }

    @Transactional
    public ApiResponse<Object> remove(UUID userId, UUID itemId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wishlist not found"));

        wl.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresent(i -> eventPublisher.publishWishlistRemove(i.getProductId(), userId));

        itemRepo.deleteById(itemId);
        wishlistRepo.save(wl);
        return new ApiResponse<>(true, "Item removed from wishlist", wl, 200);
    }

    @Transactional
    public ApiResponse<Object> clearWishlist(UUID userId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wishlist not found"));

        wl.getItems().clear();
        wishlistRepo.save(wl);

        return new ApiResponse<>(true, "Wishlist cleared", null, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> get(UUID userId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> Wishlist.builder().userId(userId).items(List.of()).build());

        return new ApiResponse<>(true, "Get wishlist", wl, 200);
    }

    @Transactional
    public ApiResponse<Object> moveToCart(UUID userId, UUID productId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wishlist not found"));

        WishlistItem item = wl.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found in wishlist"));

        UUID variantId = item.getVariantId();

        // Remove from wishlist first (prevent constraint violation if
        // product+variant already in cart and cart service returns gracefully)
        wl.getItems().remove(item);
        itemRepo.deleteById(item.getId());
        wishlistRepo.save(wl);

        // Delegate to CartService — it handles merge-if-exists and recompute
        CartItemRequest req = new CartItemRequest();
        req.setProductId(productId);
        req.setVariantId(variantId);
        req.setQuantity(1);

        ApiResponse<Object> cartResponse = cartService.addItem(req);

        if (!cartResponse.success()) {
            // Rollback: put item back in wishlist
            wl.getItems().add(item);
            wishlistRepo.save(wl);
            return new ApiResponse<>(false,
                    "Failed to add to cart: " + cartResponse.message(), null, 500);
        }

        return new ApiResponse<>(true,
                "Item moved to cart successfully", cartResponse.data(), 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getPriceDrops(UUID userId) {
        Wishlist wl = wishlistRepo.findByUserId(userId)
                .orElseGet(() -> Wishlist.builder().userId(userId).items(List.of()).build());

        List<WishlistItem> items = wl.getItems();
        if (items == null || items.isEmpty()) {
            return new ApiResponse<>(true, "No wishlist items", List.of(), 200);
        }

        // Only items with a tracked addedPrice AND a variantId can have a drop
        List<WishlistItem> trackable = items.stream()
                .filter(i -> i.getVariantId() != null && i.getAddedPrice() != null)
                .toList();

        if (trackable.isEmpty()) {
            return new ApiResponse<>(true, "No price-trackable items", List.of(), 200);
        }

        // Batch fetch all variants in one query
        List<UUID> variantIds = trackable.stream()
                .map(WishlistItem::getVariantId)
                .toList();
        Map<UUID, ProductVariant> variantMap = variantRepo.findAllById(variantIds)
                .stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        // Batch fetch product names + primary image in one query
        List<UUID> productIds = trackable.stream()
                .map(WishlistItem::getProductId)
                .distinct()
                .toList();
        // Re-use the existing projection — getName() + getDescription()
        // We also need the image; fetch raw product and pick first image attribute
        // We keep this as a single findAllById call (Spring Data does an IN query)
        Map<UUID, Product> productMap = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<PriceDropItemDto> drops = new ArrayList<>();

        for (WishlistItem wi : trackable) {
            ProductVariant variant = variantMap.get(wi.getVariantId());
            if (variant == null || variant.getPrice() == null)
                continue;

            BigDecimal added = parseSafe(wi.getAddedPrice());
            BigDecimal current = parseSafe(variant.getPrice());

            // Only report actual drops (not increases)
            if (current.compareTo(added) >= 0)
                continue;

            BigDecimal drop = added.subtract(current);
            double dropPercent = drop.divide(added, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            // Resolve display name and image cheaply from in-memory product
            Product product = productMap.get(wi.getProductId());
            String name = product != null ? product.getName() : "Unknown";

            // Grab first image attribute value for thumbnail — O(1) per product
            String image = resolveFirstImage(product);

            drops.add(PriceDropItemDto.builder()
                    .wishlistItemId(wi.getId())
                    .productId(wi.getProductId())
                    .variantId(wi.getVariantId())
                    .productName(name)
                    .imageUrl(image)
                    .addedPrice(wi.getAddedPrice())
                    .currentPrice(variant.getPrice())
                    .dropAmount(drop.toPlainString())
                    .dropPercent(Math.round(dropPercent * 10.0) / 10.0)
                    .build());
        }

        // Sort by largest drop percent first
        drops.sort(Comparator.comparingDouble(PriceDropItemDto::getDropPercent).reversed());

        return new ApiResponse<>(true,
                drops.isEmpty() ? "No price drops found" : "Price drops fetched",
                drops, 200);
    }

    private BigDecimal parseSafe(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String resolveFirstImage(Product product) {
        if (product == null)
            return null;
        return product.getProductAttributes().stream()
                .filter(pa -> pa.getCategoryAttribute() != null
                        && Boolean.TRUE.equals(pa.getCategoryAttribute().getIsImageAttribute())
                        && pa.getImages() != null
                        && !pa.getImages().isEmpty())
                .findFirst()
                .map(pa -> pa.getImages().get(0))
                .orElse(null);
    }

    @Transactional
    public ApiResponse<Object> shareWishlist(UUID userId, Integer ttlDays) {
        // Idempotent for permanent links — reuse existing token
        if (ttlDays == null) {
            Optional<SharedWishlist> existing = sharedWishlistRepo.findByOwnerIdAndExpiresAtIsNull(userId);
            if (existing.isPresent()) {
                return buildShareResponse(existing.get());
            }
        }

        String token = generateToken(12);

        ZonedDateTime expiresAt = ttlDays != null
                ? ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(ttlDays)
                : null;

        SharedWishlist share = SharedWishlist.builder()
                .ownerId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        sharedWishlistRepo.save(share);
        return buildShareResponse(share);
    }

    // Called by the public read-only controller
    @Transactional(readOnly = true)
    public ApiResponse<Object> resolveSharedWishlist(String token) {
        SharedWishlist share = sharedWishlistRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid share link"));

        if (share.getExpiresAt() != null &&
                ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).isAfter(share.getExpiresAt())) {
            return new ApiResponse<>(false, "Share link has expired", null, 410);
        }

        // Return the owner's wishlist (read-only view, no userId auth needed)
        Wishlist wl = wishlistRepo.findByUserId(share.getOwnerId())
                .orElseGet(() -> Wishlist.builder()
                        .userId(share.getOwnerId()).items(List.of()).build());

        return new ApiResponse<>(true, "Shared wishlist fetched", wl, 200);
    }

    private ApiResponse<Object> buildShareResponse(SharedWishlist share) {
        String url = baseUrl + "/public/wishlist/" + share.getToken();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shareUrl", url);
        data.put("token", share.getToken());
        data.put("expiresAt", share.getExpiresAt());
        return new ApiResponse<>(true, "Shareable link generated", data, 200);
    }

    private String generateToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TOKEN_CHARS.charAt(RNG.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}

/// juu8u8 tfty uttty7y7y tt tut6uty6t6t6rsdftggy
///
/// hyyyiyiyuiyuiyuiyuyyuiyu8i uytyy yuy7yu7y7