package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.Service.cart.WishlistService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    private UUID resolveUser(String userId) {
        return UUID.fromString(userId);
    }

    @PostMapping("/items")
    public ResponseEntity<?> add(@RequestHeader("X-User-Id") String userId,
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID variantId) {
        try {
            var wl = wishlistService.add(resolveUser(userId), productId, variantId);
            return ResponseEntity.status(200).body(wl);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> remove(@RequestHeader("X-User-Id") String userId, @PathVariable UUID itemId) {
        try {
            var wl = wishlistService.remove(resolveUser(userId), itemId);
            return ResponseEntity.status(200).body(wl);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestHeader("X-User-Id") String userId) {
        try {
            var wl = wishlistService.get(resolveUser(userId));
            return ResponseEntity.status(200).body(wl);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }
}
