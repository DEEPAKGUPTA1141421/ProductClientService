package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}

// uiujiuiujhukujihuhuihuhui huu hh gyhbhjhu huhu hhuhuhuhhj hhyuiyhui
// gyuyguvggtygtyhuuuu iuuiujiujijkkj