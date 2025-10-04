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
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository.ProductSearchDto;
import com.ProductClientService.ProductClientService.Service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/products/search")
    public List<ProductSearchDto> searchProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String attributeName,
            @RequestParam(required = false) String attributeValue) {
        return productService.searchProducts(categoryId, brandId, sellerId, attributeName, attributeValue);
    }

    @GetMapping("/category")
    public ResponseEntity<?> getCategory(@RequestParam boolean includeChildItem,
            @RequestParam Category.Level level) {
        try {
            ApiResponse<Object> response = productService.getCategory(includeChildItem, level);
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

    @GetMapping("/search")
    public ResponseEntity<?> searchProduct(@RequestParam String keyword) {
        try {
            ApiResponse<Object> response = productService.searchProducts(keyword);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
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
// njjjnkhiunm nihuif iujhiujourfhuhuir joiujiourflniuhuirf uijuiojiorf uhihiu
// huihiurfiuhijbhuyud uiyi7yiiyhui mbhgyhiyu jgyutyuu7
// huihyui hyiy7i hy7uy yuiuy78y gyutu gyutuy yugtgyu gyuguytgu