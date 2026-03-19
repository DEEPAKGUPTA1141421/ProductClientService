package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.time.ZoneId;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "brands", uniqueConstraints = @UniqueConstraint(columnNames = { "normalised_name", "category_id" }))
@Getter
@Setter
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public String normalisedName;

    public String description;
    public String logoUrl;
    public String website;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}

// kjljl n jnjl jnnjkfn jnjkf njnjgt njnt ntg ngtgt m gt
// mkmkf jrfkkm mlmklt m,, m
/*
 * product
 * has brandandcategorty
 * sosearchintentshould becreatedlikebrandandcategory,
 */