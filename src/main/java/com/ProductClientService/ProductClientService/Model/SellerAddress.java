package com.ProductClientService.ProductClientService.Model;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "seller_addresses")
public class SellerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "seller_id", referencedColumnName = "id", nullable = true)
    private Seller seller;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private User user;

    @Column(name = "kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private Kind kind = Kind.LEGAL;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "country", nullable = false)
    private String country = "IN";

    @Column(name = "pincode", nullable = false)
    private String pincode;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "adhadhar_card", nullable = true)
    private String AdhadharCard;

    @Column(name = "pan_card", nullable = true)
    private String PanCard;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    private enum Kind {
        LEGAL, PICKUP, RETURN
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}

// jhihhuhu jhkjbuhgujyug