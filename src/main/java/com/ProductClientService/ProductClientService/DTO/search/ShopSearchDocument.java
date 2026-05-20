package com.ProductClientService.ProductClientService.DTO.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * ShopSearchDocument
 * ───────────────────
 * Fully denormalized Elasticsearch document for index "shops-v1".
 *
 * One document = one ACTIVE seller (shop).
 *
 * Indexed by: ShopSearchService (on seller create / status change)
 * Queried by: ShopSearchService (nearby, text search, suggestions)
 *
 * ES mapping highlights (created by ShopsIndexInitializer):
 *   display_name              → text + keyword sub-field + search_as_you_type for suggestions
 *   location                  → geo_point  (lat/lon object format)
 *   city, category_id, status → keyword (exact filter)
 *   avg_rating                → float (range filter + sort)
 *   is_open                   → boolean (filter)
 *   ranking_score             → float (future function_score boost)
 *   logo_url                  → keyword, index:false (display only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopSearchDocument {

    // ── Identity ──────────────────────────────────────────────────────────────
    @JsonProperty("shop_id")
    private String shopId;

    // ── Searchable text ───────────────────────────────────────────────────────
    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("legal_name")
    private String legalName;

    private List<String> tags;

    // ── Filterable keyword fields ─────────────────────────────────────────────
    private String city;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    /** "ACTIVE" or "INACTIVE" — only ACTIVE shops appear in search. */
    private String status;

    @JsonProperty("is_open")
    private boolean isOpen;

    // ── Geo ───────────────────────────────────────────────────────────────────
    /**
     * ES geo_point in object format {"lat": x, "lon": y}.
     * Used for geo_distance filtering (nearby shops) and geo-distance sort.
     */
    private GeoPoint location;

    // ── Rating / ranking ──────────────────────────────────────────────────────
    @JsonProperty("avg_rating")
    private double avgRating;

    @JsonProperty("review_count")
    private int reviewCount;

    @JsonProperty("ranking_score")
    private double rankingScore;

    // ── Display (not indexed) ─────────────────────────────────────────────────
    @JsonProperty("logo_url")
    private List<String> logoUrl;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @JsonProperty("created_at")
    private Instant createdAt;

    // ── Nested geo_point ──────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        private double lat;
        private double lon;
    }
}
