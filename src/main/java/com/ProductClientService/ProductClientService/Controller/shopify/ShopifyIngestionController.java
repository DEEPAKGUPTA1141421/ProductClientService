package com.ProductClientService.ProductClientService.Controller.shopify;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyIngestionResultDto;
import com.ProductClientService.ProductClientService.Service.shopify.ShopifyIngestionService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/shopify")
@RequiredArgsConstructor
public class ShopifyIngestionController {

    private final ShopifyIngestionService shopifyIngestionService;

    /**
     * POST /api/v1/seller/shopify/ingest
     *
     * Fetches products from the configured Shopify store and ingests them into the
     * platform for the authenticated seller. The seller's JWT is all that is needed
     * —
     * the seller ID is extracted from the security principal.
     *
     * Idempotent: rows that already exist with the same external_updated_at are
     * skipped.
     * Row-level failures are reported in the response without aborting the batch.
     */
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<ShopifyIngestionResultDto>> ingestProducts(
            @AuthenticationPrincipal UserPrincipal principal) {

        ShopifyIngestionResultDto result = shopifyIngestionService.ingestProducts(principal.getId());

        String message = String.format(
                "Ingestion complete — inserted: %d, updated: %d, skipped: %d, failed: %d",
                result.inserted(), result.updated(), result.skipped(), result.failed());

        return ResponseEntity.ok(new ApiResponse<>(true, message, result, 200));
    }
}
// oijihuiui jkjkjmnjk