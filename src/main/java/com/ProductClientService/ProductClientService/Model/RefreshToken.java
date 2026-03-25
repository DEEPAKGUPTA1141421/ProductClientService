package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private UUID userId;

    // SELLER, USER, RIDER
    @Column(nullable = false)
    private String userType;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    // Track token family for rotation breach detection
    // If a revoked token from a family is reused → revoke entire family
    @Column(nullable = false)
    private String family; // UUID string, same for all tokens in a rotation chain

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public boolean isExpired() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).isAfter(this.expiresAt);
    }
}