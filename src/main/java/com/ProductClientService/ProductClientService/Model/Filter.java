package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A search filter definition.
 *
 * Two lifecycle flavours:
 *  - isSystem  = true  → created by FilterDataInitializer; cannot be deleted via API
 *  - isGlobal  = true  → always included in every category's filter response
 *                         without needing a CategoryFilterMapping row
 *  - Otherwise          → must be explicitly linked to a category via CategoryFilterMapping
 *
 * System filters (Sort, Price, Rating) are also global.
 * Custom attribute filters (Storage, Size, Color, Fabric …) are neither,
 * so the admin links them to categories when onboarding.
 */
@Entity
@Table(name = "filters",
        uniqueConstraints = @UniqueConstraint(name = "uq_filter_key", columnNames = "filter_key"))
@Getter
@Setter
public class Filter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Machine-stable identifier — used as the "id" field in FilterDto.
     * e.g.  "sort", "price", "rating", "storage", "brand"
     */
    @Column(name = "filter_key", nullable = false, length = 100)
    private String filterKey;

    /** Human-readable label shown in the UI chip. e.g. "Sort By", "Price", "Storage" */
    @Column(nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_type", nullable = false, length = 50)
    private FilterType filterType;

    /**
     * Maps directly to SearchResultsController query-param.
     * e.g. "sortBy" | "price" | "minRating" | "attributeValues" | "brandIds"
     */
    @Column(name = "param_key", nullable = false, length = 100)
    private String paramKey;

    /**
     * Only set when paramKey = "attributeValues".
     * Tells the client which attributeName to send alongside attributeValues.
     */
    @Column(name = "attribute_name", length = 200)
    private String attributeName;

    /** Lower bound for RANGE filters (null for non-RANGE). */
    @Column(name = "min_value")
    private Double minValue;

    /** Upper bound for RANGE filters (null for non-RANGE). */
    @Column(name = "max_value")
    private Double maxValue;

    /**
     * System filters cannot be deleted via admin API.
     * Only FilterDataInitializer creates these.
     */
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    /**
     * Global filters appear in every category's response without a CategoryFilterMapping row.
     * Sort, Price, and Rating are global by default.
     */
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    /** Controls render order within the filter sheet. Lower = first. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "filter",
               cascade = CascadeType.ALL,
               fetch = FetchType.EAGER,
               orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<FilterOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    public enum FilterType {
        SINGLE_SELECT,
        MULTI_SELECT,
        RANGE
    }
}
