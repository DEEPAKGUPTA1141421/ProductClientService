package com.ProductClientService.ProductClientService.Model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Scope scope; // ITEM or CART

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType; // PERCENT or FLAT

    private String discountValue;

    private String uptoAmount;

    // applicability (nullable depending on type)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Applicability applicability; // PRODUCT, BRAND, CATEGORY, CART_ALL, CART_TOTAL

    private UUID productId; // when Applicability=PRODUCT
    private UUID brandId; // when Applicability=BRAND
    private UUID categoryId; // when Applicability=CATEGORY

    private String minCartTotal;

    private ZonedDateTime startsAt;
    private ZonedDateTime endsAt;

    @Column(nullable = false)
    private Boolean active = true;

    public enum Scope {
        ITEM, CART
    }

    public enum DiscountType {
        PERCENT, FLAT
    }

    public enum Applicability {
        PRODUCT, BRAND, CATEGORY, CART_TOTAL, ITEM
    }
}

// iuji juujibhkhuibkjojuuu

// hyiuu90eipnkjuiou uuoi9 jioi.oi9oiufrhuhgv