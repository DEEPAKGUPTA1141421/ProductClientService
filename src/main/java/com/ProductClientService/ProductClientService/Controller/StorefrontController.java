package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.search.StorefrontResponse;
import com.ProductClientService.ProductClientService.Service.StorefrontService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * StorefrontController
 * ─────────────────────
 * Returns a structured, backend-ordered storefront for a seller's shop page.
 *
 * GET /api/v1/shops/{shopId}/storefront
 *
 * Authentication: JWT required (used to inject buy-again + wishlist/cart flags).
 * Anonymous requests are accepted and simply omit the BUY_AGAIN section.
 *
 * Response sections (ordered by backend):
 *   1. BUY_AGAIN  — products the user has previously ordered from this seller
 *   2. CATEGORY   — one section per unique product category, most-products first
 */
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class StorefrontController {

    private final StorefrontService storefrontService;

    @GetMapping("/{shopId}/storefront")
    public ResponseEntity<ApiResponse<StorefrontResponse>> storefront(
            @PathVariable String shopId,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal != null ? principal.getId() : null;
        StorefrontResponse response = storefrontService.getStorefront(shopId, userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Storefront fetched", response, 200));
    }
}
