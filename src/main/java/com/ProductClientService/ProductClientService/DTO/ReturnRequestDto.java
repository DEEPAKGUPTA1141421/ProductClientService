package com.ProductClientService.ProductClientService.DTO;

import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReturnRequestDto {

    private UUID id;
    private String bookingId;
    private UUID productId;
    private String reason;
    private String reasonLabel;
    private String description;
    private String status;
    private String statusLabel;
    private String adminNote;
    private List<String> evidenceImages;
    private String createdAt;
    private String updatedAt;

    // Enriched fields for the seller "Refund requests" table — populated only by
    // the seller-facing detailed query, null for the plain fromEntity() mapping.
    private String productName;
    private String categoryName;
    private String productImageUrl;
    private UUID customerId;
    private String customerName;
    private String customerAvatarUrl;
    /** Simplified two-state bucket for the seller table's pill: "New request" | "In progress" | "Refunded" | "Rejected". */
    private String boardStatusLabel;

    public static ReturnRequestDto fromEntity(ReturnRequest r) {
        return ReturnRequestDto.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
                .productId(r.getProductId())
                .reason(r.getReason().name())
                .reasonLabel(r.getReason().label())
                .description(r.getDescription())
                .status(r.getStatus().name())
                .statusLabel(r.getStatus().label())
                .adminNote(r.getAdminNote())
                .evidenceImages(r.getEvidenceImages())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .updatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null)
                .build();
    }
}
