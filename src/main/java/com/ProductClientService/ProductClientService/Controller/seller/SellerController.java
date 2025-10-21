package com.ProductClientService.ProductClientService.Controller.seller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Service.ImageUploadService;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/seller/product")
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;
    private final S3Service s3Service;
    private final ProductRepository productRepository;
    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PrivateApi
    public ResponseEntity<?> addProduct(@Valid @ModelAttribute ProductDto productDto) {
        ApiResponse<Object> response = sellerService.addProduct(productDto);
        return ResponseEntity
                .status(200)
                .body(response);
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

    // @GetMapping("/attributes/{productId}")
    // public ApiResponse<Object> getProductAttributes(@PathVariable UUID productId)
    // {
    // return sellerService.getProductAttributes(productId);
    // }

    @PostMapping("/add-variants")
    public ResponseEntity<?> addVariants(@Valid @RequestBody ProductVariantsDto dto) {
        try {
            ApiResponse<Object> response = sellerService.addProductVariants(dto);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    // @GetMapping("/get-product/{productId}")
    // public ApiResponse<Object> getVariants(@PathVariable UUID productId) {
    // return sellerService.getProductWithAttributesAndVariants(productId);
    // }

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

    @PostMapping("/upload-images")
    @PrivateApi
    public ResponseEntity<?> uploadAttributeImages(
            @RequestParam("data") String data,
            @RequestParam("step") String step,
            @RequestParam MultiValueMap<String, MultipartFile> images // handles images[0], images[1], ...
    ) {
        try {
            // Parse productAttributeIds from data string
            List<String> attributeIds = new ObjectMapper().readValue(data, new TypeReference<List<String>>() {
            });

            List<Object[]> attributeImageData = new ArrayList<>();

            // Match images[i] with attributeIds[i]
            for (int i = 0; i < attributeIds.size(); i++) {
                String key = "images[" + i + "]";
                if (images.containsKey(key)) {
                    UUID attrId = UUID.fromString(attributeIds.get(i));
                    List<MultipartFile> files = images.get(key); // directly get list
                    attributeImageData.add(new Object[] { attrId, files });
                }
            }

            ApiResponse<Object> response = sellerService.uploadAndUpdateImages(attributeImageData, step);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getShopCategories() {
        try {
            ApiResponse<Object> response = sellerService.getShopCategories();
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/by-city")
    public ResponseEntity<?> getShopsByCity(@RequestParam String city) {
        try {
            ApiResponse<Object> response = sellerService.getShopsByCity(city);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/by-city-category")
    public ResponseEntity<?> getShopsByCityAndCategory(@RequestParam String city,
            @RequestParam Seller.ShopCategory category) {
        try {
            ApiResponse<Object> response = sellerService.getShopsByCityAndCategory(city, category);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/search-shop")
    public ResponseEntity<?> searchShop(@RequestParam String keyword) {
        ApiResponse<Object> response = sellerService.searchShop(keyword);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/nearest")
    public ResponseEntity<?> getNearestShops(@RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            ApiResponse<Object> response = sellerService.getNearestShops(lat, lon, limit);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/nearest-by-category")
    public ResponseEntity<?> getNearestShopsByCategory(@RequestParam double lat,
            @RequestParam double lon,
            @RequestParam Seller.ShopCategory category,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            ApiResponse<Object> response = sellerService.getNearestShopsByCategory(lat, lon, category, limit);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }
}
// jhiu jhuiyuiu huymnkjnkhkihiyh nbuygyu bgyg bvytg mkj9oi fjnhk jhbh

// khguygu hjgjuy bnvyhtfghg hgfhty mniuhiu