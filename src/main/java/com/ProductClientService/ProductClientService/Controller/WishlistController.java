package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.cart.WishlistService;
import java.util.UUID;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @PostMapping("/items/{productId}")
    public ResponseEntity<?> add(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId) {

        return ResponseEntity.ok(wishlistService.add(getUserId(), productId, variantId));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<?> remove(@PathVariable UUID productId) {
        return ResponseEntity.ok(wishlistService.remove(getUserId(), productId));
    }

    @DeleteMapping
    public ResponseEntity<?> clearWishList() {
        return ResponseEntity.ok(wishlistService.clearWishlist(getUserId()));
    }

    @GetMapping
    public ResponseEntity<?> get() {
        return ResponseEntity.ok(wishlistService.get(getUserId()));
    }

    @PostMapping("/{productId}/move-to-cart")
    public ResponseEntity<?> moveToCart(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = wishlistService.moveToCart(getUserId(), productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 404));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 500));
        }
    }

    @GetMapping("/price-drops")
    public ResponseEntity<?> priceDrops() {
        try {
            ApiResponse<Object> response = wishlistService.getPriceDrops(getUserId());
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 500));
        }
    }

    @PostMapping("/share")
    public ResponseEntity<?> shareWishlist(
            @RequestParam(required = false) Integer ttlDays) {
        try {
            ApiResponse<Object> response = wishlistService.shareWishlist(getUserId(), ttlDays);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 500));
        }
    }

    @GetMapping("/token/{token}")
    public ResponseEntity<?> viewShared(@PathVariable String token) {
        try {
            ApiResponse<Object> response = wishlistService.resolveSharedWishlist(token);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 404));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 500));
        }
    }

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}

// uiujiuiujhukujihuhuihuhui huu hh gyhbhjhu huhu hhuhuhuhhj hhyuiyhui
// gyuyguvggtygtyhuuuu iuuiujiujijkkj