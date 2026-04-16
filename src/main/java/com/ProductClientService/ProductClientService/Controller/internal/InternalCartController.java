package com.ProductClientService.ProductClientService.Controller.internal;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal-only endpoints for service-to-service communication.
 * Protected by InternalApiKeyFilter (X-Internal-Api-Key header).
 * Never expose these routes through the public API gateway.
 */
@RestController
@RequestMapping("/internal/v1/cart")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;

    /**
     * Returns the active cart for a given user.
     * Called by Order/Payment services before checkout processing.
     *
     * Header required: X-Internal-Api-Key: <INTERNAL_API_KEY>
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Object>> getCartByUserId(@PathVariable UUID userId) {
        ApiResponse<Object> response = cartService.getCartByUserId(userId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
