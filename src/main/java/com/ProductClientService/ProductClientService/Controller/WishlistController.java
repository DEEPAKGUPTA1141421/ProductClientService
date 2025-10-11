package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.cart.WishlistService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;
    private final HttpServletRequest request;

    @PostMapping("/items/{productId}")
    @PrivateApi
    public ResponseEntity<?> add(
            @PathVariable UUID productId,
            @RequestParam(required = false) UUID variantId) {

        UUID userId = (UUID) request.getAttribute("id");
        return ResponseEntity.ok(wishlistService.add(userId, productId, variantId));
    }

    @DeleteMapping("/items/{productId}")
    @PrivateApi
    public ResponseEntity<?> remove(@PathVariable UUID productId) {
        UUID userId = (UUID) request.getAttribute("id");
        return ResponseEntity.ok(wishlistService.remove(userId, productId));
    }

    @GetMapping
    @PrivateApi
    public ResponseEntity<?> get() {
        UUID userId = (UUID) request.getAttribute("id");
        return ResponseEntity.ok(wishlistService.get(userId));
    }
}

// uiujiuiujhukujihuhuihuhui huu hh gyhbhjhu huhu hhuhuhuhhj hhyuiyhui
// gyuyguvggtygtyhuuuu iuuiujiujijkkj