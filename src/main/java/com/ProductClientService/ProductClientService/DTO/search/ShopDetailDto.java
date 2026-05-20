package com.ProductClientService.ProductClientService.DTO.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Full shop detail — returned by GET /api/v1/shops/{shopId}.
 * Includes all ShopSummaryDto fields plus extended info.
 */
@Data
@Builder
public class ShopDetailDto {

    // ── Core identity ──────────────────────────────────────────────────────────
    private UUID shopId;
    private String displayName;
    private String legalName;
    private List<String> logoUrl;

    // ── Location ───────────────────────────────────────────────────────────────
    private String city;
    private double shopLat;
    private double shopLng;

    // ── Category ───────────────────────────────────────────────────────────────
    private String categoryName;
    private UUID categoryId;

    // ── Rating / ranking ───────────────────────────────────────────────────────
    private double avgRating;
    private int reviewCount;

    // ── Status ─────────────────────────────────────────────────────────────────
    @JsonProperty("isOpen")
    private boolean isOpen;

    private List<String> tags;

    // ── Delivery enrichment ────────────────────────────────────────────────────
    private String deliveryEtaLabel;
    private double distanceKm;
    private int etaMinutes;
    private boolean sameCityDelivery;

    // ── Profile extras ─────────────────────────────────────────────────────────
    private String coverImageUrl;
    private String bio;
    private String websiteUrl;

    // ── Social / stats ─────────────────────────────────────────────────────────
    private long followerCount;

    @JsonProperty("isFollowed")
    private boolean isFollowed;

    private long totalProducts;
}
