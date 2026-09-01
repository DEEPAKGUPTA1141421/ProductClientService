package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonBackReference
    private Product product;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "price")
    private String price;

    @Column(name = "mrp")
    private String mrp;

    // Derived/effective values recomputed whenever discount config changes (or
    // the scheduled job runs). Buyer-facing search (SearchResultsRepository /
    // ProductSearchRepository) reads these directly — keep them in sync.
    @Column(name = "discount_price")
    private String discountPrice;

    @Column(name = "discount_percentage")
    private String discountPercentage;

    // ── Seller-configured discount (independent of price/stock edits) ────────
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    // Raw seller-entered number: a percentage (0-90) or a flat amount, stored as
    // String following this codebase's convention for price-like fields.
    @Column(name = "discount_value")
    private String discountValue;

    @Column(name = "discount_active")
    private Boolean discountActive = false;

    @Column(name = "discount_starts_at")
    private ZonedDateTime discountStartsAt;

    @Column(name = "discount_ends_at")
    private ZonedDateTime discountEndsAt;

    @Column(name = "stock", nullable = false)
    private int stock = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "combination", columnDefinition = "json")
    private Map<String, String> combination;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    /**
     * "Currently effective" = discountActive AND (no startsAt OR now >= startsAt)
     * AND (no endsAt OR now <= endsAt).
     */
    @Transient
    public boolean isDiscountCurrentlyEffective() {
        if (!Boolean.TRUE.equals(discountActive) || discountType == null || discountValue == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        if (discountStartsAt != null && now.isBefore(discountStartsAt)) {
            return false;
        }
        if (discountEndsAt != null && now.isAfter(discountEndsAt)) {
            return false;
        }
        return true;
    }

    /**
     * Recomputes the buyer-facing discount_price / discount_percentage columns
     * from the seller-configured discount fields. Call this any time discount
     * config is written (create/update/remove) or the scheduled job re-syncs
     * a scheduled discount's window. Does not persist by itself — caller must
     * save the entity.
     */
    public void recomputeEffectiveDiscount() {
        if (!isDiscountCurrentlyEffective()) {
            this.discountPrice = null;
            this.discountPercentage = null;
            return;
        }
        double mrpVal;
        double value;
        try {
            mrpVal = Double.parseDouble(this.mrp);
            value = Double.parseDouble(this.discountValue);
        } catch (Exception e) {
            this.discountPrice = null;
            this.discountPercentage = null;
            return;
        }
        double effectivePrice;
        double effectivePercentage;
        if (discountType == DiscountType.PERCENTAGE) {
            effectivePrice = mrpVal * (1 - value / 100.0);
            effectivePercentage = value;
        } else {
            effectivePrice = Math.max(0, mrpVal - value);
            effectivePercentage = mrpVal > 0 ? (value / mrpVal) * 100.0 : 0;
        }
        this.discountPrice = String.valueOf((long) effectivePrice);
        this.discountPercentage = String.valueOf(effectivePercentage);
    }

    /** Clears all discount config and derived values back to their defaults. */
    public void clearDiscount() {
        this.discountType = null;
        this.discountValue = null;
        this.discountActive = false;
        this.discountStartsAt = null;
        this.discountEndsAt = null;
        this.discountPrice = null;
        this.discountPercentage = null;
    }
}
// gtyguhyhuijijji mklkio huyiuuuhhhuhu