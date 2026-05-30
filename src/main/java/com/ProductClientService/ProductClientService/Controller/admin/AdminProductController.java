package com.ProductClientService.ProductClientService.Controller.admin;

import org.springframework.web.bind.annotation.RestController;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftBasicInfoDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftBrandKeywordsDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftSpecsDto;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryAttributeRequest;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryDto;
import com.ProductClientService.ProductClientService.DTO.admin.StandardProductCreateDto;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Service.admin.AdminCatalogDraftService;
import com.ProductClientService.ProductClientService.Service.admin.AdminProductService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/product")
public class AdminProductController {
    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private AdminCatalogDraftService adminCatalogDraftService;

    @PostMapping("/add-category")

    public ResponseEntity<?> addCategory(@Valid @RequestBody CategoryDto productrequest) {
        ApiResponse<Object> response = adminProductService.addCategory(productrequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/add-attribute")
    public ResponseEntity<?> addAttribute(@Valid @RequestBody AttributeDto attributerequest) {
        System.out.println("Dto Matched");
        ApiResponse<Object> response = adminProductService.addAttribute(attributerequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/update-attribute/{id}")

    public ResponseEntity<?> updateAttribute(@PathVariable UUID id, @Valid @RequestBody AttributeDto attributerequest) {
        ApiResponse<Object> response = adminProductService.updateAttributefun(id, attributerequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/add-category-from-file")
    //
    public ResponseEntity<?> test() throws IOException {
        adminProductService.addCategoryFromJsonFile();
        return ResponseEntity.status(200).body("task Queued");
    }

    @PostMapping("/create-category-attribute")
    public ResponseEntity<?> createCategoryAttribute(@RequestBody CategoryAttributeRequest request) {
        try {
            CategoryAttribute created = adminProductService.createCategoryAttribute(
                    request.categoryId(),
                    request.attributeId(),
                    request.isRequired(),
                    request.isImageAttribute(),
                    request.isVariantAttribute());
            return ResponseEntity.status(200).body(null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ── Standard Product Catalog ──────────────────────────────────────────────────

    @PostMapping("/catalog")
    public ResponseEntity<?> createCatalogEntry(@RequestBody StandardProductCreateDto dto) {
        ApiResponse<Object> response = adminProductService.createStandardProduct(dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/catalog/{id}")
    public ResponseEntity<?> updateCatalogEntry(@PathVariable UUID id,
            @RequestBody StandardProductCreateDto dto) {
        ApiResponse<Object> response = adminProductService.updateStandardProduct(id, dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/catalog/{id}/verify")
    public ResponseEntity<?> verifyCatalogEntry(@PathVariable UUID id) {
        ApiResponse<Object> response = adminProductService.verifyAndActivate(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/catalog/{id}/discontinue")
    public ResponseEntity<?> discontinueCatalogEntry(@PathVariable UUID id) {
        ApiResponse<Object> response = adminProductService.discontinue(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/{id}/go-live")
    public ResponseEntity<?> makeProductLive(@PathVariable UUID id) {
        ApiResponse<Object> response = adminProductService.makeProductLive(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ── Admin Step-by-Step Catalog Draft ─────────────────────────────────────

    /** Step 1 — create a new draft with name, description, category, EAN, productCode */
    @PostMapping("/catalog/draft")
    public ResponseEntity<?> startCatalogDraft(@Valid @RequestBody CatalogDraftBasicInfoDto dto) {
        ApiResponse<Object> response = adminCatalogDraftService.startDraft(dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Step 1 (update) — edit basic info of an existing draft */
    @PutMapping("/catalog/draft/{id}/basic-info")
    public ResponseEntity<?> updateBasicInfo(@PathVariable UUID id,
            @Valid @RequestBody CatalogDraftBasicInfoDto dto) {
        ApiResponse<Object> response = adminCatalogDraftService.updateBasicInfo(id, dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Step 2 — save key-value specifications (e.g. {"Color":"Black","RAM":"8GB"}) */
    @PutMapping("/catalog/draft/{id}/specifications")
    public ResponseEntity<?> saveDraftSpecifications(@PathVariable UUID id,
            @Valid @RequestBody CatalogDraftSpecsDto dto) {
        ApiResponse<Object> response = adminCatalogDraftService.saveSpecifications(id, dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Step 3 — upload primary product image */
    @PostMapping(value = "/catalog/draft/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDraftImage(@PathVariable UUID id,
            @RequestParam("image") MultipartFile image) {
        ApiResponse<Object> response = adminCatalogDraftService.uploadImage(id, image);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Step 4 — attach brand and set search keywords */
    @PutMapping("/catalog/draft/{id}/brand-keywords")
    public ResponseEntity<?> saveBrandAndKeywords(@PathVariable UUID id,
            @RequestBody CatalogDraftBrandKeywordsDto dto) {
        ApiResponse<Object> response = adminCatalogDraftService.saveBrandAndKeywords(id, dto);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Step 5 — verify, activate, and index to catalog-v1 */
    @PutMapping("/catalog/draft/{id}/go-live")
    public ResponseEntity<?> goLiveDraft(@PathVariable UUID id) {
        ApiResponse<Object> response = adminCatalogDraftService.goLive(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Resume — get current state of a draft */
    @GetMapping("/catalog/draft/{id}")
    public ResponseEntity<?> getDraft(@PathVariable UUID id) {
        ApiResponse<Object> response = adminCatalogDraftService.getDraft(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** List all DRAFT catalog entries (admin overview) */
    @GetMapping("/catalog/drafts")
    public ResponseEntity<?> listDrafts() {
        ApiResponse<Object> response = adminCatalogDraftService.listDrafts();
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Discard a draft */
    @DeleteMapping("/catalog/draft/{id}")
    public ResponseEntity<?> discardDraft(@PathVariable UUID id) {
        ApiResponse<Object> response = adminCatalogDraftService.discardDraft(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/catalog/search")
    public ResponseEntity<?> searchCatalog(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ApiResponse<Object> response = adminProductService.adminSearchCatalog(query, page, size);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

}
// fytdfugyuguhi yguyduhjbj jugguyge gyuguyduygjgyhguyjd gygd giyiudbhjgdiu
// jhikuhi huyiu yuu hyiuyui hyhuyiy7ui jkhuih hhiuhf huihiuhfhujj
// jkjuju huhuih hkhu