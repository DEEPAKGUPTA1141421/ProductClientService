package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ReturnRequestDto;
import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import com.ProductClientService.ProductClientService.Model.WalletTransaction;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService extends BaseService {

    private final ReturnRequestRepository returnRepository;
    private final ImageUploadService imageUploadService;
    private final FcmService fcmService;
    private final WalletService walletService;
    private final ProductRepository productRepository;

    // ── Submit a return request ───────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> submitReturn(
            String bookingId,
            ReturnRequest.ReturnReason reason,
            String description,
            UUID productId,
            List<MultipartFile> images) {

        UUID userId = getUserId();

        if (returnRepository.existsByBookingIdAndUserId(bookingId, userId)) {
            return new ApiResponse<>(false,
                    "A return request for this order already exists.", null, 409);
        }

        // Upload evidence images to Cloudinary
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    try {
                        imageUrls.add(imageUploadService.uploadImage(file.getBytes()));
                        if (imageUrls.size() == 5) break;
                    } catch (Exception e) {
                        log.warn("Evidence image upload failed for userId={}: {}", userId, e.getMessage());
                    }
                }
            }
        }

        // productId lets us resolve which seller this return concerns — optional
        // for backward compatibility with older client builds that don't send it.
        UUID sellerId = productId != null
                ? productRepository.findSellerIdByProductId(productId)
                : null;

        ReturnRequest request = ReturnRequest.builder()
                .userId(userId)
                .bookingId(bookingId)
                .productId(productId)
                .sellerId(sellerId)
                .reason(reason)
                .description(description != null ? description.strip() : null)
                .evidenceImages(imageUrls)
                .build();

        ReturnRequest saved = returnRepository.save(request);

        // Notify user — return request received
        fcmService.sendToUser(userId,
                "Return Request Submitted",
                "We've received your return request for order #" + bookingId + ". We'll review it within 24–48 hours.",
                Map.of("type", "RETURN_UPDATE", "returnId", saved.getId().toString(),
                        "status", ReturnRequest.ReturnStatus.PENDING.name()));

        return new ApiResponse<>(true, "Return request submitted successfully",
                ReturnRequestDto.fromEntity(saved), 201);
    }

    // ── Get paginated return list for current user ─────────────────────────────

    public ApiResponse<Object> getMyReturns(int page, int size) {
        UUID userId = getUserId();
        Page<ReturnRequest> pageResult = returnRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 20)));

        Map<String, Object> response = new HashMap<>();
        response.put("returns", pageResult.getContent().stream()
                .map(ReturnRequestDto::fromEntity).toList());
        response.put("totalElements", pageResult.getTotalElements());
        response.put("hasMore", pageResult.hasNext());
        response.put("page", page);

        return new ApiResponse<>(true, "Returns fetched", response, 200);
    }

    // ── Get single return detail ──────────────────────────────────────────────

    public ApiResponse<Object> getReturnById(UUID returnId) {
        UUID userId = getUserId();
        return returnRepository.findById(returnId)
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> new ApiResponse<Object>(true, "Return fetched",
                        ReturnRequestDto.fromEntity(r), 200))
                .orElse(new ApiResponse<>(false, "Return not found", null, 404));
    }

    // ── Admin: approve ────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> adminApprove(UUID returnId, String adminNote) {
        ReturnRequest r = returnRepository.findById(returnId).orElse(null);
        if (r == null) return new ApiResponse<>(false, "Return not found", null, 404);
        if (r.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            return new ApiResponse<>(false, "Return is not in PENDING state", null, 409);
        }
        r.setStatus(ReturnRequest.ReturnStatus.APPROVED);
        r.setAdminNote(adminNote);
        returnRepository.save(r);

        fcmService.sendToUser(r.getUserId(),
                "Return Approved",
                "Great news! Your return for order #" + r.getBookingId() + " has been approved. We'll schedule a pickup shortly.",
                Map.of("type", "RETURN_UPDATE", "returnId", returnId.toString(),
                        "status", ReturnRequest.ReturnStatus.APPROVED.name()));

        return new ApiResponse<>(true, "Return approved", ReturnRequestDto.fromEntity(r), 200);
    }

    // ── Admin: reject ─────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> adminReject(UUID returnId, String adminNote) {
        ReturnRequest r = returnRepository.findById(returnId).orElse(null);
        if (r == null) return new ApiResponse<>(false, "Return not found", null, 404);
        if (r.getStatus() != ReturnRequest.ReturnStatus.PENDING) {
            return new ApiResponse<>(false, "Return is not in PENDING state", null, 409);
        }
        r.setStatus(ReturnRequest.ReturnStatus.REJECTED);
        r.setAdminNote(adminNote);
        returnRepository.save(r);

        fcmService.sendToUser(r.getUserId(),
                "Return Request Update",
                "Your return request for order #" + r.getBookingId() + " could not be approved. "
                        + (adminNote != null ? adminNote : ""),
                Map.of("type", "RETURN_UPDATE", "returnId", returnId.toString(),
                        "status", ReturnRequest.ReturnStatus.REJECTED.name()));

        return new ApiResponse<>(true, "Return rejected", ReturnRequestDto.fromEntity(r), 200);
    }

    // ── Admin: advance status ─────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> adminUpdateStatus(UUID returnId, ReturnRequest.ReturnStatus newStatus) {
        ReturnRequest r = returnRepository.findById(returnId).orElse(null);
        if (r == null) return new ApiResponse<>(false, "Return not found", null, 404);
        r.setStatus(newStatus);
        returnRepository.save(r);

        // When order is refunded, credit the wallet automatically
        if (newStatus == ReturnRequest.ReturnStatus.REFUNDED) {
            // Refund amount is not stored on ReturnRequest; credit a fixed signal amount.
            // Production: look up order total from booking service and pass actual paise.
            walletService.credit(
                    r.getUserId(), 0L,
                    WalletTransaction.TransactionSource.REFUND,
                    r.getBookingId(),
                    "Refund for order #" + r.getBookingId());
        }

        String title = newStatus == ReturnRequest.ReturnStatus.REFUNDED
                ? "Refund Processed" : "Return Update";
        String body = newStatus == ReturnRequest.ReturnStatus.REFUNDED
                ? "Your refund for order #" + r.getBookingId() + " has been processed and credited to your wallet."
                : "Your return for order #" + r.getBookingId() + " status: " + newStatus.label();

        fcmService.sendToUser(r.getUserId(), title, body,
                Map.of("type", "RETURN_UPDATE", "returnId", returnId.toString(),
                        "status", newStatus.name()));

        return new ApiResponse<>(true, "Status updated", ReturnRequestDto.fromEntity(r), 200);
    }

    // ── Seller: paginated refund/return requests on their own products ────────

    private static final List<String> OPEN_STATUSES = List.of(
            "PENDING", "APPROVED", "PICKUP_SCHEDULED", "PICKED_UP");
    private static final List<String> CLOSED_STATUSES = List.of("REJECTED", "REFUNDED");
    private static final List<String> ALL_STATUSES;
    static {
        List<String> all = new ArrayList<>(OPEN_STATUSES);
        all.addAll(CLOSED_STATUSES);
        ALL_STATUSES = List.copyOf(all);
    }

    /**
     * {@code bucket}: "OPEN" (default) | "CLOSED" | "ALL" — matches the Figma
     * "Refund requests" table's Open requests / Closed requests toggle.
     */
    public ApiResponse<Object> getSellerReturns(int page, int size, String bucket) {
        UUID sellerId = getUserId();
        List<String> statuses = switch (bucket == null ? "OPEN" : bucket.toUpperCase()) {
            case "CLOSED" -> CLOSED_STATUSES;
            case "ALL"    -> ALL_STATUSES;
            default       -> OPEN_STATUSES;
        };

        Page<Object[]> pageResult = returnRepository.findSellerReturnsDetailed(
                sellerId, statuses, PageRequest.of(page, Math.min(size, 20)));

        List<ReturnRequestDto> returns = pageResult.getContent().stream()
                .map(this::toDetailedDto).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("returns", returns);
        response.put("totalElements", pageResult.getTotalElements());
        response.put("hasMore", pageResult.hasNext());
        response.put("page", page);

        return new ApiResponse<>(true, "Returns fetched", response, 200);
    }

    private ReturnRequestDto toDetailedDto(Object[] row) {
        ReturnRequest.ReturnStatus status = ReturnRequest.ReturnStatus.valueOf((String) row[2]);
        ReturnRequest.ReturnReason reason = ReturnRequest.ReturnReason.valueOf((String) row[3]);

        return ReturnRequestDto.builder()
                .id((UUID) row[0])
                .bookingId((String) row[1])
                .status(status.name())
                .statusLabel(status.label())
                .boardStatusLabel(boardStatusLabel(status))
                .reason(reason.name())
                .reasonLabel(reason.label())
                .description((String) row[4])
                .createdAt(row[5] != null ? row[5].toString() : null)
                .productId((UUID) row[6])
                .productName((String) row[7])
                .categoryName((String) row[8])
                .productImageUrl((String) row[9])
                .customerId((UUID) row[10])
                .customerName((String) row[11])
                .customerAvatarUrl((String) row[12])
                .build();
    }

    private String boardStatusLabel(ReturnRequest.ReturnStatus status) {
        return switch (status) {
            case PENDING                        -> "New request";
            case APPROVED, PICKUP_SCHEDULED,
                 PICKED_UP                       -> "In progress";
            case REFUNDED                        -> "Refunded";
            case REJECTED                        -> "Rejected";
        };
    }

    // ── Seller: open + new (last 24h) refund request counts ───────────────────

    public ApiResponse<Object> getSellerReturnSummary() {
        UUID sellerId = getUserId();
        long openCount = returnRepository.countOpenBySellerId(sellerId);
        long newCount  = returnRepository.countBySellerIdAndCreatedAtAfter(
                sellerId, java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).minusHours(24));

        return new ApiResponse<>(true, "Return summary fetched",
                Map.of("openCount", openCount, "newCount", newCount), 200);
    }
}
