package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
    @ManyToMany
    @JsonManagedReference
    @JoinTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    private Step step;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // owns FK
    @JsonManagedReference
    private Set<ProductAttribute> productAttributes = new HashSet<>();

    @Column(name = "attributes_snapshot", columnDefinition = "TEXT")
    private String attributesSnapshot;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // owns F;
    private Set<ProductVariant> variants = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductMedia> media = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_product_id")
    private StandardProduct standardProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "is_standard", nullable = false)
    private Boolean isStandard = true;

    @Column(name = "search_intent_created", nullable = false)
    private Boolean searchIntentCreated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "average_rating")
    private Double averageRating = 0.0;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    public enum Step {
        PRODUCT_NAME, // ordinal 0
        PRODUCT_ATTRIBUTE, // ordinal 1
        PRODUCT_VARIANT, // ordinal 3
        PRODUCT_IMAGE, // ordinal 2
        PRODUCT_BRAND_AND_TAGS, // ordinal 3
        LIVE, // ordinal 4
        CATALOG_SELECTED // ordinal 5 — seller picked from standard catalog (skip attributes + images)
    }
}

// jkujhkj huiuhif uiuier khuifr hiuyiru rfhiuhrg
// bhyu huihuiuh huihi huihui hkuju juj hkhuju hkh mhj bbhjuk ngjyghu bnyjgugyj
// hkuiuiuj nmuihui ygyggvgyhj nkjjuk h,khu nmhukhuk nhh