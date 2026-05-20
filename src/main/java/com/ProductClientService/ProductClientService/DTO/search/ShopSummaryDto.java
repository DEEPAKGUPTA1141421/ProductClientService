package com.ProductClientService.ProductClientService.DTO.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Returned for each shop in the listing rail (nearby + search).
 * Delivery ETA fields are enriched by ShopService via DeliveryInventoryService.
 */
@Data
@Builder
public class ShopSummaryDto {

    private UUID shopId;
    private String displayName;
    private List<String> logoUrl;
    private String city;
    private String categoryName;
    private UUID categoryId;
    private double avgRating;
    private int reviewCount;

    @JsonProperty("isOpen")
    private boolean isOpen;

    // ── Delivery enrichment ───────────────────────────────────────────────────
    /** Human-readable ETA label, e.g. "45 mins", "3 hrs", "Tomorrow". */
    private String deliveryEtaLabel;

    /** Straight-line distance from user to shop in km (from geo_distance sort). */
    private double distanceKm;
}
