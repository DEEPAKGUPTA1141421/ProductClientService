package com.ProductClientService.ProductClientService.Controller.admin;

import com.ProductClientService.ProductClientService.DTO.admin.filter.*;
import com.ProductClientService.ProductClientService.Service.admin.AdminFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only filter management.
 * All routes sit under /api/v1/admin/filters — secured via WebConfig (.anyRequest().authenticated()).
 *
 * ── Filter master data ─────────────────────────────────────────────────────
 * GET    /api/v1/admin/filters                          list all filters
 * GET    /api/v1/admin/filters/{filterId}               get single filter
 * POST   /api/v1/admin/filters                          create filter
 * PUT    /api/v1/admin/filters/{filterId}               update filter
 * DELETE /api/v1/admin/filters/{filterId}               delete filter (non-system only)
 *
 * ── Filter options ─────────────────────────────────────────────────────────
 * POST   /api/v1/admin/filters/{filterId}/options       add option to filter
 * PUT    /api/v1/admin/filters/options/{optionId}       update option
 * DELETE /api/v1/admin/filters/options/{optionId}       delete option
 *
 * ── Category ↔ filter assignment ──────────────────────────────────────────
 * GET    /api/v1/admin/categories/{categoryId}/filters              list assigned filters
 * POST   /api/v1/admin/categories/{categoryId}/filters              assign filter
 * DELETE /api/v1/admin/categories/{categoryId}/filters/{filterId}   unassign filter
 * PATCH  /api/v1/admin/categories/{categoryId}/filters/{filterId}/toggle   enable/disable
 * PATCH  /api/v1/admin/categories/{categoryId}/filters/{filterId}/order    change display order
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminFilterController {

    private final AdminFilterService adminFilterService;

    // ── Filter master data ────────────────────────────────────────────────────

    @GetMapping("/filters")
    public ResponseEntity<?> getAllFilters() {
        return ResponseEntity.ok(adminFilterService.getAllFilters());
    }

    @GetMapping("/filters/{filterId}")
    public ResponseEntity<?> getFilter(@PathVariable UUID filterId) {
        return ResponseEntity.ok(adminFilterService.getFilter(filterId));
    }

    @PostMapping("/filters")
    public ResponseEntity<?> createFilter(@Valid @RequestBody CreateFilterRequest req) {
        var response = adminFilterService.createFilter(req);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/filters/{filterId}")
    public ResponseEntity<?> updateFilter(
            @PathVariable UUID filterId,
            @RequestBody UpdateFilterRequest req) {
        var response = adminFilterService.updateFilter(filterId, req);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/filters/{filterId}")
    public ResponseEntity<?> deleteFilter(@PathVariable UUID filterId) {
        var response = adminFilterService.deleteFilter(filterId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── Filter options ────────────────────────────────────────────────────────

    @PostMapping("/filters/{filterId}/options")
    public ResponseEntity<?> addOption(
            @PathVariable UUID filterId,
            @Valid @RequestBody CreateFilterOptionRequest req) {
        var response = adminFilterService.addOption(filterId, req);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/filters/options/{optionId}")
    public ResponseEntity<?> updateOption(
            @PathVariable UUID optionId,
            @RequestBody UpdateFilterOptionRequest req) {
        var response = adminFilterService.updateOption(optionId, req);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/filters/options/{optionId}")
    public ResponseEntity<?> deleteOption(@PathVariable UUID optionId) {
        var response = adminFilterService.deleteOption(optionId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── Category ↔ filter assignment ──────────────────────────────────────────

    @GetMapping("/categories/{categoryId}/filters")
    public ResponseEntity<?> getCategoryFilters(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(adminFilterService.getCategoryFilters(categoryId));
    }

    @PostMapping("/categories/{categoryId}/filters")
    public ResponseEntity<?> assignFilter(
            @PathVariable UUID categoryId,
            @Valid @RequestBody AssignFilterRequest req) {
        var response = adminFilterService.assignFilterToCategory(categoryId, req);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/categories/{categoryId}/filters/{filterId}")
    public ResponseEntity<?> unassignFilter(
            @PathVariable UUID categoryId,
            @PathVariable UUID filterId) {
        var response = adminFilterService.unassignFilterFromCategory(categoryId, filterId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/categories/{categoryId}/filters/{filterId}/toggle")
    public ResponseEntity<?> toggleFilter(
            @PathVariable UUID categoryId,
            @PathVariable UUID filterId,
            @RequestParam boolean active) {
        var response = adminFilterService.toggleCategoryFilter(categoryId, filterId, active);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/categories/{categoryId}/filters/{filterId}/order")
    public ResponseEntity<?> updateOrder(
            @PathVariable UUID categoryId,
            @PathVariable UUID filterId,
            @RequestParam int displayOrder) {
        var response = adminFilterService.updateDisplayOrder(categoryId, filterId, displayOrder);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
