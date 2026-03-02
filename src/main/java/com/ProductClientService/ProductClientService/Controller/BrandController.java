package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.CreateBrandRequest;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.BrandService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping("/category/{categoryId}")
    public ApiResponse<Object> getBrandsByCategory(@PathVariable UUID categoryId) {
        return brandService.getBrandsByCategory(categoryId);
    }

    @GetMapping("/search")
    public ApiResponse<Object> search(
            @RequestParam String keyword,
            @RequestParam UUID categoryId) {

        return brandService.search(keyword, categoryId);
    }

    @PostMapping("/seller/brands")
    public ApiResponse<Object> create(@RequestBody CreateBrandRequest request) {
        return brandService.createBrand(request);
    }

    @GetMapping("/admin/brands/pending")
    public ApiResponse<Object> pending() {
        return brandService.pending();
    }

    @DeleteMapping("/admin/brands/{id}")
    public ApiResponse<Object> delete(@PathVariable UUID id) {
        return brandService.delete(id);
    }

    @PutMapping("/admin/brands/{id}/approve")
    public ApiResponse<Object> approve(@PathVariable UUID id) {
        return brandService.approve(id);
    }
}