package com.ProductClientService.ProductClientService.Controller.internal;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Internal-only endpoints for service-to-service communication.
 * Protected by InternalApiKeyFilter (X-Internal-Api-Key header).
 *
 * Called by the Order service after an order is confirmed to record
 * which products a user has purchased — enabling verified-buyer reviews.
 */
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepojectory userRepository;

    /**
     * Records one or more purchased product IDs for a user.
     * Safe to call multiple times — uses INSERT ON CONFLICT DO NOTHING.
     *
     * Header required: X-Internal-Api-Key: <INTERNAL_API_KEY>
     *
     * POST /internal/v1/users/{userId}/purchases
     * Body: ["uuid1", "uuid2", ...]
     */
    @PostMapping("/{userId}/purchases")
    @Transactional
    public ResponseEntity<?> recordPurchases(
            @PathVariable UUID userId,
            @RequestBody List<UUID> productIds) {

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, "User not found", null, 404));
        }

        for (UUID productId : productIds) {
            userRepository.addPurchasedProduct(userId, productId);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Purchases recorded", null, 200));
    }
}
