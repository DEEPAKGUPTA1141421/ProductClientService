package com.ProductClientService.ProductClientService.DTO.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Query parameters for GET /api/v1/search/results
 *
 * All fields are optional — omitting them returns unfiltered results.
 */
@Data
public class SearchRequest {

    // ── Core ─────────────────────────────────────────────────────────
    private String keyword;
    private UUID categoryId;

    // ── Pagination ───────────────────────────────────────────────────
    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(50)
    private int pageSize = 20;

    // ── Sorting ──────────────────────────────────────────────────────
    // rel | price_asc | price_desc | rating | newest | discount
    private String sortBy = "rel";

    // ── Price Filter ─────────────────────────────────────────────────
    private Double minPrice; // ₹
    private Double maxPrice; // ₹

    // ── Delivery Filter ──────────────────────────────────────────────
    private Boolean sameDay;
    private Boolean tomorrow;
    private Boolean freeDelivery;

    // ── Trending / Badge Filter ───────────────────────────────────────
    private Boolean bestsellers;
    private Boolean topRated;
    private Boolean newArrivals;

    // ── Brand Filter ─────────────────────────────────────────────────
    private List<UUID> brandIds;

    // ── Rating Filter ────────────────────────────────────────────────
    private Double minRating; // e.g. 4.0

    // ── Storage / Attribute Filters ──────────────────────────────────
    // Generic: attributeName → list of acceptable values
    // e.g. storage=["128 GB","256 GB"]
    private String attributeName;
    private List<String> attributeValues;

    // ── Discount Filter ──────────────────────────────────────────────
    private Integer minDiscountPercent; // e.g. 10, 20, 30, 50

    // ── Sponsored toggle (internal/admin) ────────────────────────────
    private Boolean includeSponsored = true;
}