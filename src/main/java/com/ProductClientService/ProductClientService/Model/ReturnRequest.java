package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 128)
    private String bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReturnReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    /** Cloudinary URLs for evidence photos uploaded by the user. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_images", columnDefinition = "jsonb")
    private List<String> evidenceImages = new ArrayList<>();

    /** Internal note added by admin on approval/rejection. */
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum ReturnReason {
        DEFECTIVE,
        WRONG_ITEM,
        CHANGED_MIND,
        SIZE_ISSUE,
        DAMAGED_IN_TRANSIT,
        NOT_AS_DESCRIBED,
        MISSING_PARTS,
        OTHER;

        public String label() {
            return switch (this) {
                case DEFECTIVE          -> "Defective / Not Working";
                case WRONG_ITEM         -> "Wrong Item Received";
                case CHANGED_MIND       -> "Changed My Mind";
                case SIZE_ISSUE         -> "Size / Fit Issue";
                case DAMAGED_IN_TRANSIT -> "Damaged in Transit";
                case NOT_AS_DESCRIBED   -> "Not as Described";
                case MISSING_PARTS      -> "Missing Parts / Accessories";
                case OTHER              -> "Other";
            };
        }
    }

    public enum ReturnStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PICKUP_SCHEDULED,
        PICKED_UP,
        REFUNDED;

        public String label() {
            return switch (this) {
                case PENDING          -> "Return Requested";
                case APPROVED         -> "Return Approved";
                case REJECTED         -> "Return Rejected";
                case PICKUP_SCHEDULED -> "Pickup Scheduled";
                case PICKED_UP        -> "Item Picked Up";
                case REFUNDED         -> "Refund Processed";
            };
        }
    }
}
