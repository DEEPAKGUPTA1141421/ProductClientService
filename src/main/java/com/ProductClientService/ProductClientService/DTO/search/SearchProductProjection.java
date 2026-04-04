package com.ProductClientService.ProductClientService.DTO.search;

import java.util.UUID;

/**
 * Lightweight DB projection used by the native search query.
 * All heavy lifting (sorting, filtering) happens in SQL; this
 * interface just maps the raw row into typed fields.
 */
public interface SearchProductProjection {

    UUID getProductId();

    String getProductName();

    String getBrandName();

    UUID getBrandId();

    /** Cheapest in-stock variant price (paise) */
    String getMinPrice();

    /** Original / MRP price (paise) – used to compute discount % */
    String getOriginalPrice();

    Double getAverageRating();

    Integer getRatingCount();

    /** Comma-separated image URLs from the primary image attribute */
    String getImageUrls();

    /** 1 = sponsored */
    Integer getIsSponsored();

    /** Delivery estimate in days (0 = same-day, 1 = tomorrow, etc.) */
    Integer getDeliveryDays();

    /** 1 = free delivery */
    Integer getFreeDelivery();

    /** Cheapest in-stock variant id */
    UUID getVariantId();

    /** Step = LIVE marker — already filtered in WHERE but exposed for safety */
    String getProductStep();
}