package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A single selectable option within a Filter.
 * Used for SINGLE_SELECT and MULTI_SELECT filter types.
 * RANGE filters store their bounds in the Filter entity itself and have no options.
 */
@Entity
@Table(name = "filter_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_filter_option_key",
                columnNames = {"filter_id", "option_key"}))
@Getter
@Setter
public class FilterOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_id", nullable = false)
    private Filter filter;

    /**
     * Stable machine key unique within a filter.
     * e.g.  "price_asc", "4_star", "128_gb"
     */
    @Column(name = "option_key", nullable = false, length = 100)
    private String optionKey;

    /** Display text shown in the Flutter chip/checkbox. e.g. "Price: Low to High" */
    @Column(nullable = false, length = 200)
    private String label;

    /**
     * Actual value sent in the API request.
     * e.g.  "price_asc", "4.0", "128 GB"
     */
    @Column(nullable = false, length = 500)
    private String value;

    /** Controls render order within the filter's option list. Lower = first. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
