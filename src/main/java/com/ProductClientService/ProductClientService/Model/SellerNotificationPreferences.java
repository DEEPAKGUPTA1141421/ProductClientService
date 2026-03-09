// ─────────────────────────────────────────────────────────────────────────────
// FILE: Model/SellerNotificationPreferences.java
// ─────────────────────────────────────────────────────────────────────────────
package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "seller_notification_preferences")
@Getter
@Setter
public class SellerNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    // Order
    @Column(name = "order_email")
    private boolean orderEmail = true;
    @Column(name = "order_push")
    private boolean orderPush = true;
    @Column(name = "order_sms")
    private boolean orderSms = true;

    // Payment
    @Column(name = "payment_email")
    private boolean paymentEmail = true;
    @Column(name = "payment_push")
    private boolean paymentPush = true;
    @Column(name = "payment_sms")
    private boolean paymentSms = false;

    // Stock
    @Column(name = "stock_email")
    private boolean stockEmail = true;
    @Column(name = "stock_push")
    private boolean stockPush = false;
    @Column(name = "stock_sms")
    private boolean stockSms = false;

    // Promo
    @Column(name = "promo_email")
    private boolean promoEmail = true;
    @Column(name = "promo_push")
    private boolean promoPush = false;
    @Column(name = "promo_sms")
    private boolean promoSms = false;

    // Security
    @Column(name = "security_email")
    private boolean securityEmail = true;
    @Column(name = "security_push")
    private boolean securityPush = true;
    @Column(name = "security_sms")
    private boolean securitySms = true;
}