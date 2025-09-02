package com.ProductClientService.ProductClientService.Controller.seller;

import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/seller/product")
public class SellerController {
    private final SellerService sellerService;
    private final S3Service s3Service;
    private final ProductRepository productRepository;

    public SellerController(SellerService sellerService, S3Service s3Service, ProductRepository productRepository) {
        this.s3Service = s3Service;
        this.productRepository = productRepository;
        this.sellerService = sellerService;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PrivateApi
    public ResponseEntity<?> addProduct(@Valid @ModelAttribute ProductDto productDto) {
        try {
            ApiResponse<Object> response = sellerService.addProduct(productDto);
            return ResponseEntity
                    .status(200)
                    .body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null, 501);
            System.out.println("messge" + e);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        }
    }

    @PostMapping(value = "/load-attribute")
    public ResponseEntity<?> loadAttribute(@RequestParam UUID id) {
        try {
            ApiResponse<Object> response = sellerService.loadAttribute(id);
            return ResponseEntity
                    .status(200)
                    .body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null, 501);
            System.out.println("messge" + e);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        }
    }

    @GetMapping("/getall-category-attribute/{categoryId}")
    @PrivateApi
    public ResponseEntity<?> getAttributesByCategory(@PathVariable UUID categoryId) {
        ApiResponse<Object> response = sellerService.getAttributesByCategoryId(categoryId);
        return ResponseEntity
                .status(response.statusCode())
                .body(response);
    }

    @PostMapping("/create-product-attribute")
    public ResponseEntity<?> addProductAttribute(@RequestBody ProductAttributeDto request) {
        ApiResponse<Object> response = sellerService.addProductAttribute(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/attributes/{productId}")
    public ApiResponse<Object> getProductAttributes(@PathVariable UUID productId) {
        return sellerService.getProductAttributes(productId);
    }

    @PostMapping("/add-variants")
    public ResponseEntity<?> addVariants(@Valid @RequestBody ProductVariantsDto dto) {
        try {
            ApiResponse<Object> response = sellerService.addProductVariants(dto);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/get-product/{productId}")
    public ApiResponse<Object> getVariants(@PathVariable UUID productId) {
        return sellerService.getProductWithAttributesAndVariants(productId);
    }

    @GetMapping("/make-product-live/{productId}")
    public ResponseEntity<?> MakeProductLive(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = sellerService.MakeProductLive(productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/search-product-live/{productId}")
    public ResponseEntity<?> searchProduct(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = sellerService.MakeProductLive(productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/test/{productId}")
    public ResponseEntity<?> Test(@PathVariable UUID productId, @RequestParam String keyword) {
        try {
            ApiResponse<Object> response = sellerService.searchProducts(keyword);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }
}

// ljjkjilbjhguykbhhh jhdbbhbhbh bhbhf jhujhuj jhuifhui ujfunjjfnjn