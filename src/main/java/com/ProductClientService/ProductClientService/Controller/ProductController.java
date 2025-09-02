package com.ProductClientService.ProductClientService.Controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Service.ProductService;

@RestController
@RequestMapping("/api/v1/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products/search")
    public List<Product> searchProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String attributeName,
            @RequestParam(required = false) String attributeValue) {
        return productService.searchProducts(categoryId, brandId, sellerId, attributeName, attributeValue);
    }

    @GetMapping("/category")
    public ResponseEntity<?> getCategory() {
        try {
            ApiResponse<Object> response = productService.getCategory();
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/by-parents")
    public List<Category> getCategoriesByParentIds(@RequestParam List<UUID> parentIds) {
        return productService.getCategoriesByParentIds(parentIds);
    }
}

// kkoij jkhiuh mnkjnkj kjhkjhk huh khguyg hbyu uyguj jhbuygu iyguy