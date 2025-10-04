package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_recent_searches")
@Getter
@Setter
public class UserRecentSearch {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String itemId; // product or brand ID

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemType itemType; // PRODUCT, BRAND

    private String title;
    private String imageUrl;
    private String meta; // optional extra info

    @Column(nullable = false)
    private int countOfSearch = 1;

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

    public enum ItemType {
        PRODUCT, BRAND
    }
}
