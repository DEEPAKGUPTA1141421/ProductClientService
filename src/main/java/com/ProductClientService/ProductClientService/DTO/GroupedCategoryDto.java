package com.ProductClientService.ProductClientService.DTO;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the category browse screen.
 *
 * Each entry represents a SUBCATEGORY (section heading) with its
 * SUBSUBCATEGORY children (clickable grid items).
 *
 * Returned by GET /api/v1/product/category/browse?superCategoryId={id}
 */
public record GroupedCategoryDto(
        UUID id,
        String name,
        String imageUrl,
        List<SubItem> subCategories
) {
    public record SubItem(UUID id, String name, String imageUrl) {}
}
