package com.ProductClientService.ProductClientService.Model;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    // keep applied cart-level coupons (can be many)
    @ManyToMany
    @JoinTable(name = "cart_applied_coupons", joinColumns = @JoinColumn(name = "cart_id"), inverseJoinColumns = @JoinColumn(name = "coupon_id"))
    private Set<Coupon> appliedCartCoupons = new HashSet<>();

    private String subTotal = "0";
    private String itemLevelDiscount = "0";
    private String cartLevelDiscount = "0";
    private String tax = "0";
    private String grandTotal = "0";

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    @UpdateTimestamp
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    public enum Status {
        ACTIVE, ORDERED, ABANDONED
    }
}

// kniuhyuh hgytgug huihuiunjhhhhjhjubhh