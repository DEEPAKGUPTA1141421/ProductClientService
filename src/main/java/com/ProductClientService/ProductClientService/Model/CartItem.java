package com.ProductClientService.ProductClientService.Model;

import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonIgnore
    private Cart cart;

    @Column(nullable = false)
    private UUID productId;

    private UUID variantId;

    @Column(nullable = false)
    private Integer quantity;

    private String priceAtAddition; // gross per-unit price snapshot

    // optional applied coupon for this line
    @ManyToOne
    @JoinColumn(name = "applied_coupon_id")
    private Coupon appliedCoupon;

    private String lineDiscount = "0";// from item coupon

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata; // freeform: color/size/offer etc.
}
// khuhihbhjhh