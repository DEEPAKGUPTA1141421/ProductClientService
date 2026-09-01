package com.ProductClientService.ProductClientService.Controller.seller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.Settings.AadhaarDocumentsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.AadhaarVerificationDto;
import com.ProductClientService.ProductClientService.DTO.Settings.GstDocumentDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PanDocumentDto;
import com.ProductClientService.ProductClientService.DTO.seller.CreateListingFromCatalogDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductTagRequestDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Service.AadhaarVerificationService;
import com.ProductClientService.ProductClientService.Service.SearchIntentGeneratorService;
import com.ProductClientService.ProductClientService.Service.TagService;
import com.ProductClientService.ProductClientService.Service.seller.SellerKycService;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import com.ProductClientService.ProductClientService.Service.seller.SellerAnalyticsService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private final TagService tagService;
    private final SearchIntentGeneratorService searchIntentGeneratorService;
    private final AadhaarVerificationService aadhaarVerificationService;
    private final SellerKycService sellerKycService;
    private final SellerAnalyticsService sellerAnalyticsService;
    private final com.ProductClientService.ProductClientService.Service.ReturnService returnService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> addProduct(@Valid @ModelAttribute ProductDto productDto) {
        ApiResponse<Object> response = sellerService.addProduct(productDto);
        return ResponseEntity
                .status(200)
                .body(response);
    }

    // ── GET /api/v1/seller/product/my-categories ──────────────────────────────
    // Distinct categories the seller actually has LIVE products in — used to
    // populate the product-list category filter (instead of the full global tree).
    @GetMapping("/my-categories")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMyProductCategories() {
        ApiResponse<Object> response = sellerService.getMyProductCategories();
        return ResponseEntity.status(response.statusCode()).body(response);
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

    // ── GET /api/v1/seller/product/my-products ────────────────────────────────
    // Searches Elasticsearch first; falls back to the database if ES is down/unavailable.
    @GetMapping("/my-products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMyLiveProducts(
            @RequestParam(defaultValue = "0")  int     page,
            @RequestParam(defaultValue = "50") int     size,
            @RequestParam(required = false)    String  query,
            @RequestParam(required = false)    UUID    categoryId,
            @RequestParam(required = false)    UUID    brandId,
            @RequestParam(required = false)    Double  minPrice,
            @RequestParam(required = false)    Double  maxPrice,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(required = false)    Boolean isActive,
            @RequestParam(required = false)    Integer maxStock) {
        ApiResponse<Object> response = sellerService.getMyLiveProducts(
                page, size, query, categoryId, brandId, minPrice, maxPrice, sortBy, isActive, maxStock);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/scheduled-products ──────────────────────────
    @GetMapping("/scheduled-products")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getScheduledProducts(
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "50") int    size,
            @RequestParam(required = false)    String query) {
        ApiResponse<Object> response = sellerService.getScheduledProducts(page, size, query);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── POST /api/v1/seller/product/{productId}/schedule ───────────────────────
    // Body: { "scheduledAt": "2026-09-01T10:00:00+05:30" } — also used to reschedule.
    @PostMapping("/{productId}/schedule")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> scheduleProduct(
            @PathVariable UUID productId,
            @RequestBody Map<String, String> body) {
        String raw = body.get("scheduledAt");
        java.time.ZonedDateTime scheduledAt;
        try {
            scheduledAt = java.time.ZonedDateTime.parse(raw);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(false, "Invalid scheduledAt", null, 400);
            return ResponseEntity.status(response.statusCode()).body(response);
        }
        ApiResponse<Object> response = sellerService.scheduleProduct(productId, scheduledAt);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── POST /api/v1/seller/product/{productId}/publish-now ────────────────────
    @PostMapping("/{productId}/publish-now")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> publishScheduledProductNow(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.publishScheduledProductNow(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

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

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.deleteMyProduct(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{productId}/toggle-active")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> toggleActive(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.toggleActive(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{productId}/edit-data")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getProductEditData(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.getProductEditData(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/dashboard-summary ──────────────────────────
    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getDashboardSummary() {
        ApiResponse<Object> response = sellerService.getDashboardSummary();
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {
        ApiResponse<Object> response = sellerService.getLowStockProducts(threshold);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/{productId}/variants")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getProductVariants(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.getProductVariants(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @RequestBody Map<String, Object> body) {
        Long    priceInPaise = body.get("priceInPaise") instanceof Number n ? n.longValue() : null;
        Integer stock        = body.get("stock")        instanceof Number n ? n.intValue()  : null;
        ApiResponse<Object> response = sellerService.updateVariant(productId, variantId, priceInPaise, stock);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── PATCH /api/v1/seller/product/{productId}/variants/{variantId}/discount ─
    // Configures (creates or overwrites) a per-variant discount, independently
    // of price/stock. Body: VariantDiscountDto.
    @PatchMapping("/{productId}/variants/{variantId}/discount")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> configureVariantDiscount(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody com.ProductClientService.ProductClientService.DTO.seller.VariantDiscountDto dto) {
        ApiResponse<Object> response = sellerService.configureVariantDiscount(productId, variantId, dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── DELETE /api/v1/seller/product/{productId}/variants/{variantId}/discount ─
    @DeleteMapping("/{productId}/variants/{variantId}/discount")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> removeVariantDiscount(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        ApiResponse<Object> response = sellerService.removeVariantDiscount(productId, variantId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{productId}/quick-update")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> quickUpdate(
            @PathVariable UUID productId,
            @RequestBody Map<String, Object> body) {
        String  name         = body.get("name")  instanceof String  s ? s : null;
        Long    priceInPaise = body.get("priceInPaise") instanceof Number n ? n.longValue() : null;
        Integer stock        = body.get("stock") instanceof Number  n ? n.intValue()  : null;
        ApiResponse<Object> response = sellerService.quickUpdate(productId, name, priceInPaise, stock);
        return ResponseEntity.status(response.statusCode()).body(response);
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
            @RequestParam(value = "images", required = true) MultipartFile coverFile,
            @RequestParam(value = "attributeImageKeys", required = false) List<String> attributeImageKeys,
            @RequestParam(value = "attributeImages", required = false) List<MultipartFile> attributeImages) {
        try {
            ApiResponse<Object> response = sellerService.uploadProductMedia(
                    productId, List.of(coverFile), attributeImageKeys, attributeImages);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }

    // ── Presigned direct-to-Cloudinary upload ───────────────────────────────
    // Replaces the multipart /upload-images path for the app: the client
    // gets a short-lived signature here, uploads the file straight to
    // Cloudinary itself (this server never sees the bytes), then calls
    // /media/confirm with just the resulting metadata.
    @PostMapping("/{productId}/media/signature")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMediaUploadSignature(
            @PathVariable UUID productId,
            @Valid @RequestBody com.ProductClientService.ProductClientService.DTO.seller.MediaSignatureRequestDto request) {
        ApiResponse<Object> response = sellerService.createMediaUploadSignature(productId, request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/media/confirm")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> confirmMediaUpload(
            @Valid @RequestBody com.ProductClientService.ProductClientService.DTO.seller.MediaConfirmRequestDto request) {
        ApiResponse<Object> response = sellerService.confirmMediaUpload(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @DeleteMapping("/media/remove")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> removeConfirmedMedia(
            @Valid @RequestBody com.ProductClientService.ProductClientService.DTO.seller.MediaRemoveRequestDto request) {
        ApiResponse<Object> response = sellerService.removeConfirmedMedia(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // Gate the app's "Continue" action on the Images step — 400 (complete:
    // false) if the cover or any required attribute image is still missing.
    @GetMapping("/{productId}/media/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMediaStatus(@PathVariable UUID productId) {
        ApiResponse<Object> response = sellerService.getMediaStatus(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
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

    @GetMapping("/catalog/detail/{standardProductId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getCatalogDetail(@PathVariable UUID standardProductId) {
        ApiResponse<Object> response = sellerService.getCatalogDetail(standardProductId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/listing/from-catalog")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> createListingFromCatalog(@Valid @RequestBody CreateListingFromCatalogDto dto) {
        ApiResponse<Object> response = sellerService.createListingFromCatalog(dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/activity ────────────────────────────────────
    // Weekly product activity trend (products touched / views / review comments)
    // for the seller's live products, over the last `weeks` ISO weeks.
    @GetMapping("/activity")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getProductActivity(@RequestParam(defaultValue = "2") int weeks) {
        ApiResponse<Object> response = sellerAnalyticsService.getProductActivity(weeks);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/traffic-sources ─────────────────────────────
    // Interaction counts grouped by source (home/search/pdp/push/cart) over the
    // last `days` days, scoped to the seller's live products.
    @GetMapping("/traffic-sources")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getTrafficSources(@RequestParam(defaultValue = "7") int days) {
        ApiResponse<Object> response = sellerAnalyticsService.getTrafficSources(days);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/viewers ──────────────────────────────────────
    // Distinct-session viewer count per product over the last `days` days,
    // scoped to the seller's live products.
    @GetMapping("/viewers")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getViewers(@RequestParam(defaultValue = "7") int days) {
        ApiResponse<Object> response = sellerAnalyticsService.getViewers(days);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/top-cities?days=30 ──────────────────────────
    // Real customer-location breakdown (from delivery addresses) for the
    // Customer Analytics "Top cities" card — substitutes for "Top countries"
    // since this marketplace is India-only (every address.country == "IN").
    @GetMapping("/top-cities")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getTopCities(@RequestParam(defaultValue = "30") int days) {
        ApiResponse<Object> response = sellerAnalyticsService.getTopCities(days);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/monthly-views?months=6 ───────────────────────
    @GetMapping("/monthly-views")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMonthlyViews(@RequestParam(defaultValue = "6") int months) {
        ApiResponse<Object> response = sellerAnalyticsService.getMonthlyViews(months);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/returns/summary ─────────────────────────────
    @GetMapping("/returns/summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getReturnSummary() {
        ApiResponse<Object> response = returnService.getSellerReturnSummary();
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── POST /api/v1/seller/product/customers/notify ───────────────────────────
    // Body: { "userIds": ["uuid1", ...], "message": "..." }
    @PostMapping("/customers/notify")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> notifyCustomers(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> rawIds = (List<String>) body.getOrDefault("userIds", List.of());
        List<UUID> userIds = rawIds.stream().map(UUID::fromString).toList();
        String message = (String) body.get("message");
        ApiResponse<Object> response = sellerService.notifyCustomers(userIds, message);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── GET /api/v1/seller/product/returns ──────────────────────────────────────
    @GetMapping("/returns")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getReturns(
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "20")   int    size,
            @RequestParam(defaultValue = "OPEN") String status) {
        ApiResponse<Object> response = returnService.getSellerReturns(page, size, status);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getReviews(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String query) {
        ApiResponse<Object> response = sellerService.getSellerReviews(page, size, query);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/reviews/summary")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getReviewSummary() {
        ApiResponse<Object> response = sellerService.getSellerReviewSummary();
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── DELETE /api/v1/seller/product/reviews/{reviewId} ──────────────────────
    // Removes an inappropriate/abusive comment left on one of the seller's own products.
    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> deleteReview(@PathVariable UUID reviewId) {
        ApiResponse<Object> response = sellerService.deleteSellerReview(reviewId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── POST /api/v1/seller/product/reviews/{reviewId}/reply ──────────────────
    // Body: { "reply": "Thanks for the feedback!" }
    @PostMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> replyToReview(@PathVariable UUID reviewId, @RequestBody Map<String, String> body) {
        ApiResponse<Object> response = sellerService.replySellerReview(reviewId, body.get("reply"));
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── POST /api/v1/seller/product/reviews/{reviewId}/react ──────────────────
    // Body: { "emoji": "😊" } — posting the same emoji again clears the reaction.
    @PostMapping("/reviews/{reviewId}/react")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> reactToReview(@PathVariable UUID reviewId, @RequestBody Map<String, String> body) {
        ApiResponse<Object> response = sellerService.reactToSellerReview(reviewId, body.get("emoji"));
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

    // KYC Documents (Aadhaar mandatory, PAN mandatory, GST optional)

    @GetMapping("/kyc/documents")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getKycDocuments() {
        ApiResponse<Object> response = sellerKycService.getKycStatus();
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping(value = "/kyc/documents/aadhaar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> submitAadhaarDocuments(@Valid @ModelAttribute AadhaarDocumentsDto dto) {
        try {
            ApiResponse<Object> response = sellerKycService.submitAadhaar(dto);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error submitting Aadhaar details: " + e.getMessage(), null, 500));
        }
    }

    @PostMapping(value = "/kyc/documents/pan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> submitPanDocument(@Valid @ModelAttribute PanDocumentDto dto) {
        try {
            ApiResponse<Object> response = sellerKycService.submitPan(dto);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error submitting PAN details: " + e.getMessage(), null, 500));
        }
    }

    @PostMapping(value = "/kyc/documents/gst", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> submitGstDocument(@Valid @ModelAttribute GstDocumentDto dto) {
        try {
            ApiResponse<Object> response = sellerKycService.submitGst(dto);
            return ResponseEntity.status(response.statusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Error submitting GST details: " + e.getMessage(), null, 500));
        }
    }

}

// jhiu jhuiyuiu huymnkjnkhkihiyh nbuygyu bgyg bvytg mkj9oi fjnhk jhbh
// jjijjioi hjuhjijhkijoijijjhjhk
