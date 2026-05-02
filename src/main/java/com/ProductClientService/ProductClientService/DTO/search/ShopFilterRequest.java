package com.ProductClientService.ProductClientService.DTO.search;

import lombok.Data;

import java.util.UUID;

/**
 * Query parameters for shop listing endpoints.
 *
 * Bound via @ModelAttribute / @RequestParam on ShopController.
 *
 * sortBy values: "distance" | "rating" | "name"
 * (delivery-time sort is approximated as distance — ETA is computed post-query)
 */
@Data
public class ShopFilterRequest {

    // ── User location (required for geo filter + delivery ETA) ────────────────
    private double userLat;
    private double userLng;

    // ── Text search (optional) ────────────────────────────────────────────────
    private String keyword;

    // ── Filters (optional) ────────────────────────────────────────────────────
    private UUID categoryId;
    private Double minRating;

    /**
     * Maximum delivery estimate in minutes.
     * Applied post-ES in ShopService (not an ES filter).
     * Values: 60, 120, 1440 (< 1 hr, < 2 hrs, < 1 day).
     */
    private Integer maxDeliveryMinutes;

    // ── Sort + pagination ──────────────────────────────────────────────────────
    private String sortBy = "distance";
    private int page = 0;
    private int pageSize = 20;

    /** Geo search radius in km. Default 50 km. */
    private double radiusKm = 50.0;
}
