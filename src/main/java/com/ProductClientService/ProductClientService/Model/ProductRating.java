package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "product_ratings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "user_id"}))
@Getter
@Setter
public class ProductRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating; // 1-5 stars

    @Column(length = 100)
    private String title; // review headline

    @Column(length = 2000)
    private String review; // optional body text

    /** Cloudinary URLs uploaded with the review. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 1024)
    private List<String> reviewImages = new ArrayList<>();

    /** Incremented via Kafka consumer — denormalized for fast sort/display. */
    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;

    /** True when the reviewer has a confirmed purchase of this product. */
    @Column(name = "verified_purchase", nullable = false)
    private boolean verifiedPurchase = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}