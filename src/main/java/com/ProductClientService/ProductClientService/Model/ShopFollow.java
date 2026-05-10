package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "shop_follows",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "seller_id"})
)
@Getter
@Setter
public class ShopFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @CreationTimestamp
    @Column(name = "followed_at", updatable = false)
    private ZonedDateTime followedAt;
}
