package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "standard_products")
@Getter
@Setter
public class StandardProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Barcode (EAN-13 / UPC) — primary lookup key for catalog search
    @Column(name = "ean", length = 13, unique = true)
    private String ean;

    // Internal catalog code e.g. MAGGI-70G
    @Column(name = "product_code", length = 50, unique = true)
    private String productCode;

    // Denormalized JSON snapshot of all specifications — avoids joins on seller reads
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    // First image URL stored flat for fast catalog search results
    @Column(name = "primary_image_url", columnDefinition = "TEXT")
    private String primaryImageUrl;

    // Comma-separated extra keywords to improve search recall (e.g. "noodles,instant,2-min")
    @Column(name = "search_keywords", columnDefinition = "TEXT")
    private String searchKeywords;

    // Admin must explicitly verify before sellers can list against this entry
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.DRAFT;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_product_id")
    private Set<ProductImage> productImages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brandEntity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public enum Status {
        DRAFT, ACTIVE, DISCONTINUED
    }
}