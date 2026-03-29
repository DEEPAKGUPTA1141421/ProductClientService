package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "shared_wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The user who shared
    @Column(nullable = false)
    private UUID ownerId;

    // The short token that appears in the URL
    @Column(nullable = false, unique = true, length = 16)
    private String token;

    // TTL: null means never expires
    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @CreationTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}