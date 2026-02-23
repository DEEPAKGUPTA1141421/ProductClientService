package com.ProductClientService.ProductClientService.Model;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;


@Entity
@Table(name = "stock_notifications")
@Getter
@Setter
public class StockNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant productVariant;

    private boolean notified = false; // so we don’t notify twice
}
