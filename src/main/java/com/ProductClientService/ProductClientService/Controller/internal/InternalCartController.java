package com.ProductClientService.ProductClientService.Controller.internal;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Address;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Internal-only endpoints for service-to-service communication.
 * Protected by InternalApiKeyFilter (X-Internal-Api-Key header).
 * Never expose these routes through the public API gateway.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;
    private final SellerAddressRepository sellerAddressRepository;

    /**
     * Returns the active cart for a given user.
     * Called by Order/Payment services before checkout processing.
     */
    @GetMapping("/cart/{userId}")
    public ResponseEntity<ApiResponse<Object>> getCartByUserId(@PathVariable UUID userId) {
        ApiResponse<Object> response = cartService.getCartByUserId(userId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /**
     * Returns address coordinates and text for a saved address UUID.
     * Used by the payment/booking service to resolve delivery address lat/lng.
     */
    @GetMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<Object>> getAddress(@PathVariable UUID addressId) {
        return sellerAddressRepository.findById(addressId)
                .map(addr -> {
                    Map<String, Object> body = Map.of(
                            "id",      addressId,
                            "line1",   addr.getLine1() != null ? addr.getLine1() : "",
                            "city",    addr.getCity()  != null ? addr.getCity()  : "",
                            "state",   addr.getState() != null ? addr.getState() : "",
                            "lat",     addr.getLatitude()  != null ? addr.getLatitude().doubleValue()  : 0.0,
                            "lng",     addr.getLongitude() != null ? addr.getLongitude().doubleValue() : 0.0
                    );
                    return ResponseEntity.ok(new ApiResponse<Object>(true, "ok", body, 200));
                })
                .orElse(ResponseEntity.status(404)
                        .body(new ApiResponse<>(false, "Address not found", null, 404)));
    }
}
