package com.ProductClientService.ProductClientService.DTO.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryFiltersResponse {

    /** The category for which these filters apply. */
    private UUID categoryId;

    /** Display name of the category. */
    private String categoryName;

    /**
     * Whether filters were inherited from a parent category because this
     * category has no attributes of its own configured.
     */
    private boolean inheritedFromParent;

    /**
     * Ordered list of filters to render.
     * Fixed filters (Sort, Price, Rating) always come first,
     * followed by dynamic attribute filters for this category.
     */
    private List<FilterDto> filters;
}
