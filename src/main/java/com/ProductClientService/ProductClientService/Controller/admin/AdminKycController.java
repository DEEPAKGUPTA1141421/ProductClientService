package com.ProductClientService.ProductClientService.Controller.admin;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.admin.AdminKycService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;

import lombok.RequiredArgsConstructor;

/**
 * AdminKycController
 * ──────────────────
 * Admin-only endpoints to review seller-submitted Aadhaar/PAN/GST documents
 * and authorize (or reject) the shop.
 *
 * GET   /api/v1/admin/sellers/kyc/pending           — list sellers awaiting review
 * GET   /api/v1/admin/sellers/kyc/{sellerId}         — full (decrypted) submission detail
 * PATCH /api/v1/admin/sellers/kyc/{sellerId}/approve — approve KYC + authorize shop
 * PATCH /api/v1/admin/sellers/kyc/{sellerId}/reject  — reject KYC with a reason
 */
@RestController
@RequestMapping("/api/v1/admin/sellers/kyc")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminKycController {

    private final AdminKycService adminKycService;

    @GetMapping("/pending")
    public ResponseEntity<?> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        ApiResponse<Object> response = adminKycService.listPending(pageable);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{sellerId}")
    public ResponseEntity<?> getDetail(@PathVariable UUID sellerId) {
        ApiResponse<Object> response = adminKycService.getDetail(sellerId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{sellerId}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID sellerId) {
        ApiResponse<Object> response = adminKycService.approve(sellerId, currentAdminId());
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{sellerId}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID sellerId, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse<>(false, "A rejection reason is required", null, 400));
        }
        ApiResponse<Object> response = adminKycService.reject(sellerId, currentAdminId(), reason);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    private UUID currentAdminId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getId();
    }
}
