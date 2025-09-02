package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Service.BrandService;
import org.springframework.web.bind.annotation.*;

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
}