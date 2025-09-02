package com.ProductClientService.ProductClientService.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.SectionRequest;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.SectionItem;
import com.ProductClientService.ProductClientService.Service.SectionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/{category}")
    public ResponseEntity<?> getSections(@PathVariable String category) {
        try {
            List<Section> sections = sectionService.getSectionsByCategory(category);
            ApiResponse<Object> response = new ApiResponse<>(
                    true, "Sections fetched successfully", sections, 200);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(
                    false, "Failed to fetch sections", e.getMessage(), 500);
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @GetMapping("/{sectionId}/items")
    public ResponseEntity<?> getSectionItems(@PathVariable UUID sectionId) {
        try {
            List<SectionItem> items = sectionService.getItemsForSection(sectionId);
            ApiResponse<Object> response = new ApiResponse<>(
                    true, "Items fetched successfully", items, 200);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(
                    false, "Failed to fetch items", e.getMessage(), 500);
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<?> createSection(@RequestBody SectionRequest request) {
        try {
            Section section = sectionService.createSection(request);
            ApiResponse<Object> response = new ApiResponse<>(
                    true, "Section created successfully", section, 201);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(
                    false, "Failed to create section", e.getMessage(), 400);
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSection(@PathVariable UUID id, @RequestBody SectionRequest request) {
        try {
            Section section = sectionService.updateSection(id, request);
            ApiResponse<Object> response = new ApiResponse<>(
                    true, "Section updated successfully", section, 200);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(
                    false, "Failed to update section", e.getMessage(), 400);
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }
}
// lkjoijijjjkjjoiujiojioj jkbiuhi