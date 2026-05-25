package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter

public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public String name;

    private Integer externalId;

    @Column(name = "hsn_code", length = 10)
    private String hsnCode;

    @Column(name = "gst_rate")
    private Double gstRate; // default GST % for this category

    private Integer min_products = 1;

    private Integer max_products = 9;
    @Column(nullable = true, name = "category_level")
    private Level categoryLevel;
    @Column(name = "special_rules", columnDefinition = "TEXT")
    private String specialRules;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<CategoryAttribute> categoryAttributes = new java.util.ArrayList<>();

    // getters and setters
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    // Getters and setters (recommended for private fields)
    public ZonedDateTime getCreatedAt() {
        return createdAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }

    public enum Level {
        SUPER_CATEGORY,
        CATEGORY,
        SUBCATEGORY,
        SUBSUBCATEGORY
    }

}
