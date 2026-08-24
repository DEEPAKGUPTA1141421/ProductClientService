package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ProductClientService.ProductClientService.Utils.AesStringConverter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seller_kyc_documents")
@Getter
@Setter
public class SellerKycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private Seller seller;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "aadhaar_front_url")
    private String aadhaarFrontUrl;

    @Column(name = "aadhaar_back_url")
    private String aadhaarBackUrl;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "pan_document_url")
    private String panDocumentUrl;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "gst_document_url")
    private String gstDocumentUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private KycStatus status = KycStatus.IN_PROGRESS;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private ZonedDateTime reviewedAt;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public enum KycStatus {
        IN_PROGRESS,
        PENDING,
        APPROVED,
        REJECTED
    }
}
