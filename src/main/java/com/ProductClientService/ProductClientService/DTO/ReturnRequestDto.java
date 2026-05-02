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
    private String reason;
    private String reasonLabel;
    private String description;
    private String status;
    private String statusLabel;
    private String adminNote;
    private List<String> evidenceImages;
    private String createdAt;
    private String updatedAt;

    public static ReturnRequestDto fromEntity(ReturnRequest r) {
        return ReturnRequestDto.builder()
                .id(r.getId())
                .bookingId(r.getBookingId())
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
