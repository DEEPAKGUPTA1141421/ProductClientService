package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.CreateBrandRequest;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Service.BrandService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping("/category/{categoryId}")
    public List<Brand> getBrandsByCategory(@PathVariable UUID categoryId) {
        return brandService.getBrandsByCategory(categoryId);
    }

    @GetMapping("/search")
    public List<Brand> search(
            @RequestParam String keyword,
            @RequestParam UUID categoryId) {

        return brandService.search(keyword, categoryId);
    }

    @PostMapping("/seller/brands")
    public Brand create(@RequestBody CreateBrandRequest request) {
        return brandService.createBrand(request);
    }

    @GetMapping("/admin/brands/pending")
    public List<Brand> pending() {
        return brandService.pending();
    }

    @DeleteMapping("/admin/brands/{id}")
    public void delete(@PathVariable UUID id) {
        brandService.delete(id);
    }

    @PutMapping("/admin/brands/{id}/approve")
    public void approve(@PathVariable UUID id) {
        brandService.approve(id);
    }
}