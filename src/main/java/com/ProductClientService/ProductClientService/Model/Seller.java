package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// getting the error
@Entity
@Table(name = "sellers")
@Getter
@Setter
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "status")
    private String status;

    @Column(name = "risk_tier")
    private String riskTier = "LOW";

    @Column(name = "onboarding_stage")
    @Enumerated(EnumType.ORDINAL)
    private ONBOARDSTAGE onboardingStage = ONBOARDSTAGE.RESGISTER;

    @OneToOne(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "seller_id", unique = true)
    @JsonManagedReference
    private Address address;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "profile_imgae_and_videos", columnDefinition = "TEXT")
    private String profileImageAndVideos; // This can be a JSON string or a comma-separated list of URLs

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public enum ONBOARDSTAGE {
        RESGISTER,
        LOCATION,
        BASIC_INFO_NAME,
        BUSINESS_INFO,
        BANK_ACCOUNT,
        ADHADHAR_CARD,
        DOCUMENT_VERIFICATION_PENDING,
        DOCUMENT_VERIFIED
    }
}

// skihyiyhhiujhhuhuyjhjhjbgj mlnjjhjhh jji hkuuojui kihuihui kjhj bhhu
// khlhju hkhu bhu ghhui ghyuyui ghih ggyfg hjhjnjkji uhkkjjjkjnjk
// huui uouo uiij uhiui huikj hkuuih jkadd