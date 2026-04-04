package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchRequest;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.Service.SearchResultsService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * SearchResultsController
 * ────────────────────────
 *
 * GET /api/v1/search/results
 *
 * Fully authenticated endpoint (JWT required).
 * The userId from the JWT is used ONLY for wishlist flag injection —
 * it is never leaked into cache keys or response metadata.
 *
 * All query params are optional; omitting them returns the full live
 * product catalogue sorted by relevance.
 *
 * Examples
 * ─────────
 * # Basic keyword search, page 0
 * GET /api/v1/search/results?keyword=iphone&page=0&pageSize=20
 *
 * # Filter by brand + sort by price
 * GET /api/v1/search/results?keyword=phone
 * &brandIds=<uuid1>&brandIds=<uuid2>
 * &sortBy=price_asc
 *
 * # 20%+ discount, top-rated only
 * GET /api/v1/search/results?keyword=shoes
 * &minDiscountPercent=20&minRating=4.0
 *
 * # Storage attribute filter (256 GB or 512 GB)
 * GET /api/v1/search/results?keyword=phone
 * &attributeName=storage&attributeValues=256 GB&attributeValues=512 GB
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchResultsController {

    private final SearchResultsService searchService;

    /**
     * Primary search + filter endpoint for the Flutter search results page.
     *
     * @param req       All filter/sort/pagination params (validated by @Valid)
     * @param principal Injected from JWT — null only if somehow unauthenticated
     *                  (Spring Security will 401 before reaching here for protected
     *                  routes)
     */
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<SearchResultsResponse>> searchResults(
            @Valid SearchRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        UUID userId = principal != null ? principal.getId() : null;

        SearchResultsResponse response = searchService.search(req, userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Search results fetched", response, 200));
    }

    /**
     * Cache eviction endpoint — admin/internal use only.
     * Call this after bulk product imports or price updates.
     *
     * Security: restrict to ROLE_ADMIN in WebConfig.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> evictCache(
            @RequestParam(required = false) UUID categoryId) {

        if (categoryId != null) {
            searchService.evictCategoryCache(categoryId);
        }
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cache evicted", null, 200));
    }
}