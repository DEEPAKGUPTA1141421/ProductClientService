package com.ProductClientService.ProductClientService.Controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.Section;
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

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = productService.getProductDetail(productId);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/get-section/{category}")
    public ResponseEntity<?> getSections(@PathVariable String category) {
        try {
            System.out.println("Category: " + category);
            ApiResponse<Object> response = productService.getSectionsByCategory(category);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    // Add rating
    @PostMapping("/add-rating/{productId}")
    public ResponseEntity<?> addRating(
            @PathVariable UUID productId,
            @RequestParam UUID userId,
            @RequestParam int rating,
            @RequestParam(required = false) String review) {
        ApiResponse<Object> response = productService.createOrUpdateRating(productId, userId, rating, review);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Get all ratings
    @GetMapping("/get-rating/{productId}")
    public ResponseEntity<?> getRatings(@PathVariable UUID productId) {
        ApiResponse<Object> response = productService.getRatingsByProduct(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Get summary
    @GetMapping("/get-rating-summary/{productId}/summary")
    public ResponseEntity<?> getRatingSummary(@PathVariable UUID productId) {
        ApiResponse<Object> response = productService.getRatingSummary(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
// ,mniuhiu nhg8iy hjguytu jhgutututtgttggttt fytftf huhuh mknihuih jhbyujhjuhju
// hgjygjy hmgjygyjg hbjh mlhiuhn ihiuh hiuhihuibhnbhjnk hnknk hjioj nknjnh bjk
// njjkhgi hjguyg hguy hjguy bjhg hjgjygnmbgjh khuhnkhib hh hh