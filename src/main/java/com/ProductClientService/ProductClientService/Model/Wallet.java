package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets", indexes = @Index(name = "idx_wallet_user_id", columnList = "user_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    /** Balance stored in paise (1 INR = 100 paise) to avoid floating-point errors. */
    @Column(name = "balance_paise", nullable = false)
    @Builder.Default
    private Long balancePaise = 0L;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    /** Optimistic lock to prevent concurrent deductions from corrupting balance. */
    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
