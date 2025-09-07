package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.Service.cart.CartService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    private UUID resolveUser(String userId) {
        return UUID.fromString(userId);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestHeader("X-User-Id") String userId, @RequestBody CartItemRequest req) {
        try {
            ApiResponse<Object> response = cartService.addItem(resolveUser(userId), req);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateQty(@RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId, @RequestParam int qty) {
        try {
            ApiResponse<Object> response = cartService.updateQuantity(resolveUser(userId), itemId, qty);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@RequestHeader("X-User-Id") String userId, @PathVariable UUID itemId) {
        try {
            var cart = cartService.removeItem(resolveUser(userId), itemId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader("X-User-Id") String userId) {
        try {
            var cart = cartService.getCart(resolveUser(userId));
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clear(@RequestHeader("X-User-Id") String userId) {
        try {
            var cart = cartService.clearCart(resolveUser(userId));
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    // ---- Coupons ----
    @PostMapping("/items/{itemId}/coupon/{code}")
    public ResponseEntity<?> applyItemCoupon(@RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId, @PathVariable String code) {
        try {
            var cart = cartService.applyItemCoupon(resolveUser(userId),
                    new ApplyCouponRequest() {
                        {
                            setItemId(itemId);
                            setCode(code);
                        }
                    });
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}/coupon")
    public ResponseEntity<?> removeItemCoupon(@RequestHeader("X-User-Id") String userId,
            @PathVariable UUID itemId) {
        try {
            var cart = cartService.removeItemCoupon(resolveUser(userId), itemId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PostMapping("/coupons/{code}")
    public ResponseEntity<?> applyCartCoupon(@RequestHeader("X-User-Id") String userId, @PathVariable String code) {
        try {
            var cart = cartService.applyCartCoupon(resolveUser(userId), code);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/coupons/{code}")
    public ResponseEntity<?> removeCartCoupon(@RequestHeader("X-User-Id") String userId, @PathVariable String code) {
        try {
            var cart = cartService.removeCartCoupon(resolveUser(userId), code);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }
}
