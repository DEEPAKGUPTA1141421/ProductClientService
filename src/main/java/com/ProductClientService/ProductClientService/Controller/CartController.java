package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.ApplyCouponRequest;
import com.ProductClientService.ProductClientService.DTO.Cart.CartItemRequest;
import com.ProductClientService.ProductClientService.Service.cart.CartService;
import java.util.UUID;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final HttpServletRequest request;

    @PostMapping("/items")

    public ResponseEntity<?> addItem(@RequestBody CartItemRequest req) {
        try {
            ApiResponse<Object> response = cartService.addItem(req);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PutMapping("/items/{itemId}")

    public ResponseEntity<?> updateQty(
            @PathVariable UUID itemId, @RequestParam int qty) {
        try {
            ApiResponse<Object> response = cartService.updateQuantity(itemId, qty);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}")

    public ResponseEntity<?> removeItem(@PathVariable UUID itemId) {
        try {
            ApiResponse<Object> response = cartService.removeItem(itemId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/get-cart")

    public ResponseEntity<?> getCart() {
        try {
            var cart = cartService.getCart();
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping

    public ResponseEntity<?> clear() {
        try {
            var cart = cartService.clearCart();
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    // ---- Coupons ----
    @PostMapping("/items/{itemId}/coupon/{code}")

    public ResponseEntity<?> applyItemCoupon(
            @PathVariable UUID itemId, @PathVariable String code) {
        try {
            var cart = cartService.applyItemCoupon(
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

    public ResponseEntity<?> removeItemCoupon(
            @PathVariable UUID itemId) {
        try {
            var cart = cartService.removeItemCoupon(itemId);
            return ResponseEntity.status(200).body(cart);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PostMapping("/coupons/{code}")
    public ResponseEntity<ApiResponse<Object>> applyCartCoupon(@PathVariable String code) {

        return ResponseEntity.status(200).body(cartService.applyCartCoupon(code));
    }

    @GetMapping("/coupons")

    public ResponseEntity<?> getApplicableCoupons(HttpServletRequest request) {
        return ResponseEntity.ok(cartService.getApplicableCoupons());
    }

    @DeleteMapping("/coupons/{code}")

    public ResponseEntity<?> removeCartCoupon(@PathVariable String code) {
        return ResponseEntity.status(200).body(cartService.removeCartCoupon(code));
    }

}
// jiokooioljiiiii