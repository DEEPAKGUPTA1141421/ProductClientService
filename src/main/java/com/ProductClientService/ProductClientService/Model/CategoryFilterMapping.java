package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Links a category to a non-global Filter.
 *
 * When the admin onboards a new category (e.g. "Jeans"), they create rows here
 * mapping it to filters like Size, Color, Fabric.
 * Global filters (Sort, Price, Rating) are excluded — they always appear automatically.
 *
 * Hierarchy rule: CategoryFilterService walks up the parent chain if this
 * category has no active mappings of its own.
 */
@Entity
@Table(name = "category_filter_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_category_filter",
                columnNames = {"category_id", "filter_id"}))
@Getter
@Setter
public class CategoryFilterMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Loaded eagerly so CategoryFilterService can build the DTO without extra queries. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filter_id", nullable = false)
    private Filter filter;

    /** Controls render order after global filters. Lower = appears first. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** Soft-disable a filter for a category without deleting the mapping. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
