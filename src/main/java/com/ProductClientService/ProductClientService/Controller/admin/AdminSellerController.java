package com.ProductClientService.ProductClientService.Controller.admin;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Service.SellerQrService;
import com.ProductClientService.ProductClientService.Service.ShopIndexer;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * AdminSellerController
 * ──────────────────────
 * Admin-only endpoints for managing seller lifecycle.
 *
 * PATCH /api/v1/admin/sellers/{sellerId}/activate — approve seller → ACTIVE,
 * index shop
 * PATCH /api/v1/admin/sellers/{sellerId}/deactivate — suspend seller →
 * INACTIVE, remove shop
 * POST /api/v1/admin/sellers/{sellerId}/reindex — force re-index existing
 * seller
 */
@RestController
@RequestMapping("/api/v1/admin/sellers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSellerController {

    private final SellerRepository sellerRepository;
    private final ShopIndexer shopIndexer;
    private final EventPublisherService eventPublisher;
    private final SellerQrService sellerQrService;

    @PatchMapping("/{sellerId}/activate")
    public ResponseEntity<ApiResponse<Map<String, String>>> activate(@PathVariable UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

        try {
            seller.setStatus("ACTIVE");
            String qrCodeUrl = sellerQrService.generateAndUploadSellerQr(seller);
            seller.setQrCodeUrl(qrCodeUrl);
        } catch (Exception e) {
            throw new RuntimeException("Seller QR generation failed: " + e.getMessage(), e);
        }

        sellerRepository.save(seller);

        // Index immediately (async) + publish event for fan-out consumers
        shopIndexer.indexSeller(sellerId);
        eventPublisher.publishSellerLive(sellerId);

        Map<String, String> data = Map.of(
                "sellerId", seller.getId().toString(),
                "sellerPageUrl", sellerQrService.buildSellerPageUrl(seller),
                "qrCodeUrl", seller.getQrCodeUrl());

        return ResponseEntity.ok(new ApiResponse<>(true, "Seller activated, QR generated, and shop indexed", data, 200));
    }

    @PatchMapping("/{sellerId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

        seller.setStatus("INACTIVE");
        sellerRepository.save(seller);
        shopIndexer.deindexSeller(sellerId);

        return ResponseEntity.ok(new ApiResponse<>(true, "Seller deactivated and shop removed", null, 200));
    }

    @PostMapping("/{sellerId}/reindex")
    public ResponseEntity<ApiResponse<Void>> reindex(@PathVariable UUID sellerId) {
        shopIndexer.indexSeller(sellerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Shop re-index triggered", null, 200));
    }
}
