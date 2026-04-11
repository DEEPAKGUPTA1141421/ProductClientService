package com.ProductClientService.ProductClientService.DTO.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterDto {

    /** Stable unique key for this filter (e.g. "price", "rating", or an attribute UUID). */
    private String id;

    /** Human-readable label shown in the UI chip or sheet header. */
    private String label;

    /**
     * How the Flutter UI should render this filter.
     * SINGLE_SELECT → radio / exclusive chip (Sort, Rating)
     * MULTI_SELECT  → checkbox / multi-chip (Brand, Storage, Color)
     * RANGE         → dual-thumb slider (Price)
     */
    private FilterType filterType;

    /**
     * SearchRequest query-param key that this filter maps to.
     * Examples: "sortBy", "minRating", "brandIds", "attributeValues", "price"
     */
    private String paramKey;

    /**
     * Only set for attribute-based filters (paramKey = "attributeValues").
     * Tells the Flutter side which attributeName to send alongside attributeValues.
     */
    private String attributeName;

    /** Selectable options for SINGLE_SELECT and MULTI_SELECT filters. Empty for RANGE. */
    private List<FilterOptionDto> options;

    /** Lower bound for RANGE filters (Price: 0). Null for non-RANGE. */
    private Double minValue;

    /** Upper bound for RANGE filters (Price: 1 000 000). Null for non-RANGE. */
    private Double maxValue;

    public enum FilterType {
        SINGLE_SELECT,
        MULTI_SELECT,
        RANGE
    }
}
