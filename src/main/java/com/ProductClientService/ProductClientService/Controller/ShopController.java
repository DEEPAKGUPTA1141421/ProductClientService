package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.search.ShopDetailDto;
import com.ProductClientService.ProductClientService.DTO.search.ShopFilterRequest;
import com.ProductClientService.ProductClientService.DTO.search.ShopPageResponse;
import com.ProductClientService.ProductClientService.Service.ShopFollowService;
import com.ProductClientService.ProductClientService.Service.ShopService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

/**
 * ShopController
 * ───────────────
 *
 * All shop listing and detail endpoints consumed by the Flutter app.
 * No authentication required — shops are publicly browsable.
 *
 * Endpoints
 * ──────────
 *  GET /api/v1/shops/nearby        — geo-distance listing (primary tab)
 *  GET /api/v1/shops/search        — keyword + filter search
 *  GET /api/v1/shops/suggestions   — autocomplete for search bar
 *  GET /api/v1/shops/{shopId}      — shop detail page
 *
 * Common query params for listing endpoints
 * ──────────────────────────────────────────
 *  userLat          (double)   — user delivery latitude
 *  userLng          (double)   — user delivery longitude
 *  categoryId       (UUID)     — filter by shop category
 *  minRating        (double)   — e.g. 4.0
 *  maxDeliveryMinutes (int)    — 60 | 120 | 1440
 *  sortBy           (string)   — distance | rating | name
 *  page             (int)      — 0-based page index
 *  pageSize         (int)      — default 20
 *  radiusKm         (double)   — geo search radius, default 50
 *
 * Examples
 * ─────────
 *  GET /api/v1/shops/nearby?userLat=12.97&userLng=77.59&page=0
 *  GET /api/v1/shops/search?keyword=bakery&userLat=12.97&userLng=77.59&minRating=4.0
 *  GET /api/v1/shops/suggestions?q=bak&limit=6
 *  GET /api/v1/shops/abc-123?userLat=12.97&userLng=77.59
 */
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final ShopFollowService shopFollowService;

    // ── Nearby listing ────────────────────────────────────────────────────────

    /**
     * Primary shop listing — ACTIVE shops sorted by distance from the user.
     * Called by the Flutter ShopScreen on load and on filter change.
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<ShopPageResponse>> nearby(ShopFilterRequest req) {
        ShopPageResponse page = shopService.getNearbyShops(req);
        return ResponseEntity.ok(new ApiResponse<>(true, "Shops fetched", page, 200));
    }

    // ── Text search ───────────────────────────────────────────────────────────

    /**
     * Keyword + filter shop search backed by Elasticsearch.
     * Falls back to listing all ACTIVE shops when keyword is blank.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ShopPageResponse>> search(ShopFilterRequest req) {
        ShopPageResponse page = shopService.searchShops(req);
        return ResponseEntity.ok(new ApiResponse<>(true, "Search results fetched", page, 200));
    }

    // ── Autocomplete suggestions ──────────────────────────────────────────────

    /**
     * Returns up to {@code limit} (default 6) shop name suggestions
     * matching the given prefix, powered by search_as_you_type.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> suggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "6") int limit) {

        List<String> suggestions = shopService.getSuggestions(q, limit);
        return ResponseEntity.ok(new ApiResponse<>(true, "Suggestions fetched", suggestions, 200));
    }

    // ── Shop detail ───────────────────────────────────────────────────────────

    /**
     * Returns full shop detail enriched with delivery ETA.
     * Auth is optional — isFollowed is populated when user is logged in.
     */
    @GetMapping("/{shopId}")
    public ResponseEntity<ApiResponse<ShopDetailDto>> shopDetail(
            @PathVariable String shopId,
            @RequestParam(defaultValue = "0.0") double userLat,
            @RequestParam(defaultValue = "0.0") double userLng) {

        UUID userId = getOptionalUserId();
        ShopDetailDto detail = shopService.getShopDetail(shopId, userLat, userLng, userId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Shop not found", null, 404));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Shop detail fetched", detail, 200));
    }

    // ── Follow / Unfollow ─────────────────────────────────────────────────────

    @PostMapping("/{shopId}/follow")
    public ResponseEntity<ApiResponse<Void>> follow(@PathVariable String shopId) {
        UUID userId = getAuthenticatedUserId();
        shopFollowService.follow(userId, UUID.fromString(shopId));
        return ResponseEntity.ok(new ApiResponse<>(true, "Followed", null, 200));
    }

    @DeleteMapping("/{shopId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollow(@PathVariable String shopId) {
        UUID userId = getAuthenticatedUserId();
        shopFollowService.unfollow(userId, UUID.fromString(shopId));
        return ResponseEntity.ok(new ApiResponse<>(true, "Unfollowed", null, 200));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID getAuthenticatedUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getId();
    }

    private UUID getOptionalUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                return p.getId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
