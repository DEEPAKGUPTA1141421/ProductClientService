package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.HashSet;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(length = 255)
    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Address> addresses = new ArrayList<>();

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "gender")
    private String gender;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    /**
     * Flat list of purchased product UUIDs stored as a jsonb array on the users row.
     * No separate table — just a column. Populated by the order service.
     * Used to gate review submission without any join.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchased_product_ids", columnDefinition = "jsonb")
    private Set<UUID> purchasedProductIds = new HashSet<>();

    /** UUIDs of the user's active CartItems — synced on every cart write. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cart_item_ids", columnDefinition = "jsonb")
    private Set<UUID> cartItemIds = new HashSet<>();

    /** UUIDs of the user's WishlistItems — synced on every wishlist write. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wishlist_item_ids", columnDefinition = "jsonb")
    private Set<UUID> wishlistItemIds = new HashSet<>();

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

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        BLOCKED,
        PENDING_VERIFICATION
    }
}
// njkjnjkkjjkkljjijiliji