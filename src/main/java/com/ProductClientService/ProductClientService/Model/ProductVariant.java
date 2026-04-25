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

    @Column(name = "discount_price")
    private String discountPrice;

    @Column(name = "discount_percentage")
    private String discountPercentage;

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
}
// gtyguhyhuijijji mklkio huyiuuuhhhuhu