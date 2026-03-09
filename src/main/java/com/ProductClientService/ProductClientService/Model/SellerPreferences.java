// ─────────────────────────────────────────────────────────────────────────────
// FILE: Model/SellerPreferences.java
// ─────────────────────────────────────────────────────────────────────────────
package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "seller_preferences")
@Getter
@Setter
public class SellerPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "language")
    private String language = "English";

    @Column(name = "theme")
    private String theme = "Light";

    @Column(name = "currency")
    private String currency = "₹ INR (Indian Rupee)";

    @Column(name = "time_zone")
    private String timeZone = "Asia/Kolkata (IST)";
}