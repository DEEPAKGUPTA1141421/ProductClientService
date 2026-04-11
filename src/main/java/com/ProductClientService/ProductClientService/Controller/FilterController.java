package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.filter.CategoryFiltersResponse;
import com.ProductClientService.ProductClientService.Service.CategoryFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * FilterController
 * ─────────────────
 * Public endpoint — no authentication required.
 * Flutter calls this once per category and caches the result locally.
 *
 * GET /api/v1/categories/{categoryId}/filters
 *   → Returns the ordered filter list for the given category.
 *   → If the category has no attributes, filters are inherited from the
 *     nearest ancestor that does (indicated by inheritedFromParent = true).
 *
 * DELETE /api/v1/categories/{categoryId}/filters/cache  (admin only — secured separately)
 *   → Evicts the Redis cache entry so the next request rebuilds it.
 *     Call this from admin tooling after updating category attributes.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class FilterController {

    private final CategoryFilterService categoryFilterService;

    @GetMapping("/{categoryId}/filters")
    public ResponseEntity<ApiResponse<CategoryFiltersResponse>> getFilters(
            @PathVariable UUID categoryId) {

        CategoryFiltersResponse data = categoryFilterService.getFiltersForCategory(categoryId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Filters fetched successfully", data, 200));
    }

    @DeleteMapping("/{categoryId}/filters/cache")
    public ResponseEntity<ApiResponse<Void>> evictCache(
            @PathVariable UUID categoryId) {

        categoryFilterService.evictFilterCache(categoryId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Filter cache evicted for categoryId=" + categoryId, null, 200));
    }
}
