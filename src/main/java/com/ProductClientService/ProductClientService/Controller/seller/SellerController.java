package com.ProductClientService.ProductClientService.Controller.seller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.Settings.AadhaarVerificationDto;
import com.ProductClientService.ProductClientService.DTO.seller.CatalogSearchResultDto;
import com.ProductClientService.ProductClientService.DTO.seller.CreateListingFromCatalogDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductTagRequestDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Service.AadhaarVerificationService;
import com.ProductClientService.ProductClientService.Service.ImageUploadService;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.SearchIntentGeneratorService;
//import com.ProductClientService.ProductClientService.Service.SuggestionGeneratorService;
import com.ProductClientService.ProductClientService.Service.TagService;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import com.cloudinary.Search;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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
import org.springframework.security.access.prepost.PreAuthorize;

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
    private final TagService tagService;
    private final SearchIntentGeneratorService searchIntentGeneratorService;
    private final AadhaarVerificationService aadhaarVerificationService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> addProduct(@Valid @ModelAttribute ProductDto productDto) {
        ApiResponse<Object> response = sellerService.addProduct(productDto);
        return ResponseEntity
                .status(200)
                .body(response);
    }

    @PostMapping("/attach-brand")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> AttachBrandToProduct(
            @RequestParam UUID productId,
            @RequestParam UUID brandId) {
        ApiResponse<Object> response = sellerService.attachBrandToProduct(productId,
                brandId);
        return ResponseEntity
                .status(200)

                .body(response);
    }

    @GetMapping("/draft-product")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> draftProduct() {
        ApiResponse<Object> response = sellerService.getLatestDraftProduct();
        return ResponseEntity.status(200).body(response);
    }

    /**
     * Returns the seller's current draft product with ALL step data populated.
     * Frontend uses this to resume product creation from where the seller left off.
     */
    @GetMapping("/draft-product/full")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getDraftProductFull() {
        try {
            ApiResponse<Object> response = sellerService.getDraftProductFull();
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/discard-draft-product")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> discardDraftProduct() {
        ApiResponse<Object> response = sellerService.discardDraftProduct();
        return ResponseEntity.status(200).body(response);
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
    public ResponseEntity<?> getAttributesByCategory(@PathVariable UUID categoryId) {
        System.out.println("Category ID: " + categoryId); // Debug log
        ApiResponse<Object> response = sellerService.getAttributesByCategoryId(categoryId);
        return ResponseEntity
                .status(response.statusCode())
                .body(response);
    }

    @PostMapping(value = "/create-product-attribute", consumes = MediaType.APPLICATION_JSON_VALUE)

    public ResponseEntity<?> addProductAttribute(@RequestBody ProductAttributeDto request) {
        ApiResponse<Object> response = sellerService.addProductAttribute(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/add-tag")
    //
    public ResponseEntity<?> addTag(@Valid @RequestBody ProductTagRequestDto request) {
        tagService.AddProductTag(request);
        ApiResponse<Object> response = new ApiResponse<>(true, "Tags added successfully", null, 200);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/{productId}/tags/{tagId}")
    public ResponseEntity<?> removeTag(
            @PathVariable UUID productId,
            @PathVariable UUID tagId) {

        tagService.removeTagFromProduct(productId, tagId);
        ApiResponse<Object> response = new ApiResponse<>(true, "Tag removed successfully", null, 200);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(@RequestParam UUID productId) {
        searchIntentGeneratorService.generateForProduct(productId);
        ApiResponse<Object> response = new ApiResponse<>(true, "Test completed",
                null, 200);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping(value = "/update-address")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateAddress(@RequestBody SellerBasicInfo infoRequest) {
        try {
            ApiResponse<Object> response = sellerService.handleLocation(infoRequest);
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

    // @GetMapping("/attributes/{productId}")
    // public ApiResponse<Object> getProductAttributes(@PathVariable UUID productId)
    // {
    // return sellerService.getProductAttributes(productId);
    // }

    @PostMapping("/add-variants")
    @PreAuthorize("hasRole('SELLER')")
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
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> MakeProductLive(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = sellerService.MakeProductLive(productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/search-product-live/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> searchProduct(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = sellerService.MakeProductLive(productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @GetMapping("/test/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> Test(@PathVariable UUID productId, @RequestParam String keyword) {
        try {
            ApiResponse<Object> response = sellerService.searchProducts(keyword);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(501).body(e.getMessage());
        }
    }

    @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> uploadProductMedia(
            @RequestParam("productId") UUID productId,
            @RequestParam(value = "images", required = false) List<MultipartFile> coverFiles,
            @RequestParam(value = "attributeImageKeys", required = false) List<String> attributeImageKeys,
            @RequestParam(value = "attributeImages", required = false) List<MultipartFile> attributeImages) {
        try {
            ApiResponse<Object> response = sellerService.uploadProductMedia(
                    productId, coverFiles, attributeImageKeys, attributeImages);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/media/{mediaId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> removeProductMedia(@PathVariable UUID mediaId) {
        try {
            ApiResponse<Object> response = sellerService.removeProductMedia(mediaId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PatchMapping("/media/{mediaId}/set-cover")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> setCoverImage(@PathVariable UUID mediaId) {
        try {
            ApiResponse<Object> response = sellerService.setCoverImage(mediaId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/{productId}/media")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getProductMedia(@PathVariable UUID productId) {
        try {
            ApiResponse<Object> response = sellerService.getProductMedia(productId);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
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

    // @GetMapping("/by-city-category")
    // public ResponseEntity<?> getShopsByCityAndCategory(@RequestParam String city,
    // @RequestParam Seller.ShopCategory category) {
    // try {
    // ApiResponse<Object> response = sellerService.getShopsByCityAndCategory(city,
    // category);
    // return ResponseEntity.status(response.statusCode()).body(response);
    // } catch (Exception e) {
    // return ResponseEntity.status(501).body(e.getMessage());
    // }
    // }

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

    // @GetMapping("/nearest-by-category")
    // public ResponseEntity<?> getNearestShopsByCategory(@RequestParam double lat,
    // @RequestParam double lon,
    // @RequestParam Seller.ShopCategory category,
    // @RequestParam(defaultValue = "4") int limit) {
    // try {
    // ApiResponse<Object> response = sellerService.getNearestShopsByCategory(lat,
    // lon, category, limit);
    // return ResponseEntity.status(response.statusCode()).body(response);
    // } catch (Exception e) {
    // return ResponseEntity.status(501).body(e.getMessage());
    // }
    // }

    // ── Standard Product Catalog flow ────────────────────────────────────────────

    @GetMapping("/catalog/search")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> searchCatalog(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ApiResponse<Object> response = sellerService.searchCatalog(query, page, size);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/listing/from-catalog")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> createListingFromCatalog(@RequestBody CreateListingFromCatalogDto dto) {
        ApiResponse<Object> response = sellerService.createListingFromCatalog(dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Aadhaar Verification Endpoints

    @PostMapping("/kyc/aadhar/send-otp")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> sendAadhaarOtp(@Valid @RequestBody AadhaarVerificationDto request) {
        try {
            ApiResponse<Object> response = aadhaarVerificationService.triggerAadhaarOtp(request);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error sending OTP: " + e.getMessage(), null, 500));
        }
    }

    @PostMapping("/aadhaar/verify-otp")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> verifyAadhaarOtp(
            @RequestParam String otp) {
        try {
            ApiResponse<Object> response = aadhaarVerificationService.verifyAadhaarOtp(otp);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error verifying OTP: " + e.getMessage(), null, 500));
        }
    }

}

// jhiu jhuiyuiu huymnkjnkhkihiyh nbuygyu bgyg bvytg mkj9oi fjnhk jhbh
// kiyui nhuihu uihyiu hjh nhjhj hjhj bhjhj hkhu hyihu hjhj hjhiujnjnjnn
// hyuihu huihk khiurf guihrfbk hukhur jhbrkf fgrtt tgte tggrrerehjjuhyh
// uhiui nhuuhiuhhu huh hj hkj hukhukjhukhu hukkjk nhk
// jhh jhhjhbb hj jkj hujkj hkj jkbhjhhuhu uhhumjbhhj hjhuj hbhjuk khhuk
// hjhukjkjjihukjbhjkjk jkkj njj
