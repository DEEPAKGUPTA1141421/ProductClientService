package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.Service.cart.CartService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final HttpServletRequest request;

    @PostMapping("/items")
    @PrivateApi
    public ResponseEntity<?> addItem(@RequestBody CartItemRequest req) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            ApiResponse<Object> response = cartService.addItem(userId, req);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PutMapping("/items/{itemId}")
    @PrivateApi
    public ResponseEntity<?> updateQty(
            @PathVariable UUID itemId, @RequestParam int qty) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            ApiResponse<Object> response = cartService.updateQuantity(userId, itemId, qty);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}")
    @PrivateApi
    public ResponseEntity<?> removeItem(@PathVariable UUID itemId) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            ApiResponse<Object> response = cartService.removeItem(userId, itemId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/get-cart")
    @PrivateApi
    public ResponseEntity<?> getCart() {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.getCart(userId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping
    @PrivateApi
    public ResponseEntity<?> clear() {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.clearCart(userId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    // ---- Coupons ----
    @PostMapping("/items/{itemId}/coupon/{code}")
    @PrivateApi
    public ResponseEntity<?> applyItemCoupon(
            @PathVariable UUID itemId, @PathVariable String code) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.applyItemCoupon(userId,
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
    @PrivateApi
    public ResponseEntity<?> removeItemCoupon(
            @PathVariable UUID itemId) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.removeItemCoupon(userId, itemId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PostMapping("/coupons/{code}")
    @PrivateApi
    public ResponseEntity<?> applyCartCoupon(@PathVariable String code) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.applyCartCoupon(userId, code);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/coupons")
    public ResponseEntity<?> getApplicableCoupons(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("id");
        return ResponseEntity.ok(cartService.getApplicableCoupons(userId));
    }

    @DeleteMapping("/coupons/{code}")
    @PrivateApi
    public ResponseEntity<?> removeCartCoupon(@PathVariable String code) {
        try {
            UUID userId = (UUID) request.getAttribute("id");
            var cart = cartService.removeCartCoupon(userId, code);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }
}

// jmjfnnfnfnfnfnfn nfjfnrnrnfnfn njjv jvjfjfjfjfj

// nhkhu huihu jj njnjhh hhhhiuuihui gyuyhyyhyuhyuhyuhy