package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.time.ZoneId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "brands")
@Data
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public String name;
    public String description;
    public String logoUrl;
    public String website;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @Column(name = "category_id", nullable = false)
    private UUID categoryId; // 👈 new column

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public ZonedDateTime getCreatedAt() {
        return createdAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }
}