package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions",
       indexes = @Index(name = "idx_wallet_tx_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    /** Amount in paise — always positive; direction is expressed via {@link TransactionType}. */
    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 24)
    private TransactionSource source;

    /** Optional back-reference: bookingId for PURCHASE/REFUND, returnId for REFUND, etc. */
    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(length = 256)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    // ── Enums ──────────────────────────────────────────────────────────────────

    public enum TransactionType {
        CREDIT, DEBIT;

        public String label() {
            return this == CREDIT ? "Money Added" : "Money Spent";
        }
    }

    public enum TransactionSource {
        REFUND,
        PURCHASE,
        ADMIN_CREDIT,
        ADMIN_DEBIT,
        CASHBACK,
        REFERRAL;

        public String label() {
            return switch (this) {
                case REFUND       -> "Refund";
                case PURCHASE     -> "Purchase";
                case ADMIN_CREDIT -> "Added by Support";
                case ADMIN_DEBIT  -> "Adjusted by Support";
                case CASHBACK     -> "Cashback";
                case REFERRAL     -> "Referral Bonus";
            };
        }
    }
}
