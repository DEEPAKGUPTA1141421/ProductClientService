package com.ProductClientService.ProductClientService.DTO.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * StandardCatalogDocument
 * ────────────────────────
 * Denormalized Elasticsearch document for index "catalog-v1".
 *
 * One document = one verified + active StandardProduct.
 *
 * Indexed by: StandardProductIndexer  (on first boot via seeder, on admin create/update)
 * Queried by: SellerService.searchCatalog
 *
 * Mapping highlights:
 *   name, description, search_keywords → text  (full-text + fuzziness)
 *   name, brand_name, category_name   → .keyword sub-field (exact filter/sort)
 *   ean, product_code                 → keyword (barcode / code exact lookup)
 *   category_id, brand_id             → keyword (filter)
 *   is_verified, status               → boolean / keyword (mandatory pre-filter)
 *   primary_image_url, specifications → keyword, index=false (display only)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandardCatalogDocument {

    // ── Identity ──────────────────────────────────────────────────────────────
    @JsonProperty("catalog_id")
    private String catalogId;           // StandardProduct UUID as string → ES _id

    // ── Full-text search fields ───────────────────────────────────────────────
    private String name;
    private String description;

    @JsonProperty("search_keywords")
    private String searchKeywords;      // comma-separated keywords (e.g. "noodles,instant,2-min")

    // ── Exact / barcode lookup ────────────────────────────────────────────────
    private String ean;                 // EAN-13 / UPC barcode
    @JsonProperty("product_code")
    private String productCode;         // internal code e.g. "MAGGI-70G"

    // ── Filterable facets ─────────────────────────────────────────────────────
    @JsonProperty("brand_id")
    private String brandId;

    @JsonProperty("brand_name")
    private String brandName;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("category_name")
    private String categoryName;

    // ── Lifecycle flags (used as mandatory must-filters) ──────────────────────
    @JsonProperty("is_verified")
    private Boolean isVerified;

    private String status;              // StandardProduct.Status enum name

    // ── Display-only (not indexed, returned in _source) ───────────────────────
    @JsonProperty("primary_image_url")
    private String primaryImageUrl;

    private String specifications;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @JsonProperty("created_at")
    private Instant createdAt;
}
