package com.ProductClientService.ProductClientService.Service.admin;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftBasicInfoDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftBrandKeywordsDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftResponseDto;
import com.ProductClientService.ProductClientService.DTO.admin.CatalogDraftSpecsDto;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Service.seller.StandardProductIndexer;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AdminCatalogDraftService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final StandardProductRepository standardProductRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StandardProductIndexer standardProductIndexer;
    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;

    // ── Step 1: Create draft with basic info ──────────────────────────────────

    @Transactional
    public ApiResponse<Object> startDraft(CatalogDraftBasicInfoDto dto) {
        if (dto.ean() != null && !dto.ean().isBlank()
                && standardProductRepository.existsByEan(dto.ean())) {
            return new ApiResponse<>(false, "A catalog entry with this EAN already exists", null, 409);
        }
        if (dto.productCode() != null && !dto.productCode().isBlank()
                && standardProductRepository.existsByProductCode(dto.productCode())) {
            return new ApiResponse<>(false, "A catalog entry with this product code already exists", null, 409);
        }

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        StandardProduct sp = new StandardProduct();
        sp.setName(dto.name().trim());
        sp.setDescription(dto.description());
        sp.setCategory(category);
        sp.setEan(dto.ean());
        sp.setProductCode(dto.productCode());
        sp.setStatus(StandardProduct.Status.DRAFT);
        sp.setIsVerified(false);
        sp.setDraftStep(StandardProduct.DraftStep.BASIC_INFO);

        StandardProduct saved = standardProductRepository.save(sp);
        log.info("Admin started catalog draft id={} name={}", saved.getId(), saved.getName());
        return new ApiResponse<>(true, "Draft created", Map.of("id", saved.getId()), 201);
    }

    // ── Step 1 (update): Edit basic info of an existing draft ────────────────

    @Transactional
    public ApiResponse<Object> updateBasicInfo(UUID id, CatalogDraftBasicInfoDto dto) {
        StandardProduct sp = findDraft(id);

        if (dto.ean() != null && !dto.ean().equals(sp.getEan())
                && standardProductRepository.existsByEan(dto.ean())) {
            return new ApiResponse<>(false, "A catalog entry with this EAN already exists", null, 409);
        }
        if (dto.productCode() != null && !dto.productCode().equals(sp.getProductCode())
                && standardProductRepository.existsByProductCode(dto.productCode())) {
            return new ApiResponse<>(false, "A catalog entry with this product code already exists", null, 409);
        }

        sp.setName(dto.name().trim());
        sp.setDescription(dto.description());
        sp.setEan(dto.ean());
        sp.setProductCode(dto.productCode());

        if (dto.categoryId() != null) {
            Category category = categoryRepository.findById(dto.categoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            sp.setCategory(category);
        }

        standardProductRepository.save(sp);
        return new ApiResponse<>(true, "Basic info updated", null, 200);
    }

    // ── Step 2: Save specifications (key-value map serialized to JSON) ────────

    @Transactional
    public ApiResponse<Object> saveSpecifications(UUID id, CatalogDraftSpecsDto dto) {
        StandardProduct sp = findDraft(id);

        try {
            String specsJson = objectMapper.writeValueAsString(dto.specifications());
            sp.setSpecifications(specsJson);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Invalid specifications: " + e.getMessage(), null, 400);
        }

        if (sp.getDraftStep().ordinal() < StandardProduct.DraftStep.SPECIFICATIONS.ordinal()) {
            sp.setDraftStep(StandardProduct.DraftStep.SPECIFICATIONS);
        }

        standardProductRepository.save(sp);
        return new ApiResponse<>(true, "Specifications saved", null, 200);
    }

    // ── Step 3: Upload primary image to Cloudinary ────────────────────────────

    @Transactional
    public ApiResponse<Object> uploadImage(UUID id, MultipartFile file) {
        StandardProduct sp = findDraft(id);

        String ct = file.getContentType() != null ? file.getContentType() : "";
        if (!ALLOWED_IMAGE_TYPES.contains(ct)) {
            return new ApiResponse<>(false,
                    "Image must be JPEG, PNG, or WEBP. Got: " + ct, null, 400);
        }

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "catalog/" + id + "/cover"));

            sp.setPrimaryImageUrl(uploadResult.get("secure_url").toString());

            if (sp.getDraftStep().ordinal() < StandardProduct.DraftStep.IMAGE.ordinal()) {
                sp.setDraftStep(StandardProduct.DraftStep.IMAGE);
            }

            standardProductRepository.save(sp);
            return new ApiResponse<>(true, "Image uploaded",
                    Map.of("primaryImageUrl", sp.getPrimaryImageUrl()), 200);

        } catch (Exception e) {
            log.error("Cloudinary upload failed for catalog draft id={}: {}", id, e.getMessage());
            return new ApiResponse<>(false, "Image upload failed: " + e.getMessage(), null, 500);
        }
    }

    // ── Step 4: Set brand and search keywords ─────────────────────────────────

    @Transactional
    public ApiResponse<Object> saveBrandAndKeywords(UUID id, CatalogDraftBrandKeywordsDto dto) {
        StandardProduct sp = findDraft(id);

        if (dto.brandId() != null) {
            Brand brand = brandRepository.findById(dto.brandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            sp.setBrandEntity(brand);
        }

        if (dto.searchKeywords() != null) {
            sp.setSearchKeywords(dto.searchKeywords().trim());
        }

        if (sp.getDraftStep().ordinal() < StandardProduct.DraftStep.BRAND_KEYWORDS.ordinal()) {
            sp.setDraftStep(StandardProduct.DraftStep.BRAND_KEYWORDS);
        }

        standardProductRepository.save(sp);
        return new ApiResponse<>(true, "Brand and keywords saved", null, 200);
    }

    // ── Step 5: Go live — verify, activate, index to catalog-v1 ──────────────

    @Transactional
    public ApiResponse<Object> goLive(UUID id) {
        StandardProduct sp = findDraft(id);

        if (sp.getName() == null || sp.getName().isBlank()) {
            return new ApiResponse<>(false, "Product name is required before going live", null, 400);
        }
        if (sp.getCategory() == null) {
            return new ApiResponse<>(false, "Category is required before going live", null, 400);
        }

        sp.setIsVerified(true);
        sp.setStatus(StandardProduct.Status.ACTIVE);
        sp.setDraftStep(StandardProduct.DraftStep.LIVE);

        StandardProduct saved = standardProductRepository.save(sp);

        standardProductIndexer.indexProduct(saved);
        log.info("Admin catalog draft id={} is now live and indexed", id);

        return new ApiResponse<>(true, "Catalog entry is now live and indexed to catalog-v1",
                Map.of("id", saved.getId(), "name", saved.getName()), 200);
    }

    // ── GET draft state (resume) ───────────────────────────────────────────────

    public ApiResponse<Object> getDraft(UUID id) {
        StandardProduct sp = standardProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        return new ApiResponse<>(true, "Draft fetched", toResponseDto(sp), 200);
    }

    // ── GET all drafts (admin overview) ───────────────────────────────────────

    public ApiResponse<Object> listDrafts() {
        List<CatalogDraftResponseDto> drafts = standardProductRepository
                .findByStatusOrderByCreatedAtDesc(StandardProduct.Status.DRAFT)
                .stream()
                .map(this::toResponseDto)
                .toList();
        return new ApiResponse<>(true, "Drafts fetched", drafts, 200);
    }

    // ── Discard draft ─────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> discardDraft(UUID id) {
        StandardProduct sp = standardProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        if (sp.getStatus() != StandardProduct.Status.DRAFT) {
            return new ApiResponse<>(false, "Only DRAFT entries can be discarded", null, 400);
        }

        standardProductRepository.delete(sp);
        log.info("Admin discarded catalog draft id={}", id);
        return new ApiResponse<>(true, "Draft discarded", null, 200);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StandardProduct findDraft(UUID id) {
        StandardProduct sp = standardProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));
        if (sp.getStatus() != StandardProduct.Status.DRAFT) {
            throw new RuntimeException("This catalog entry is already live or discontinued");
        }
        return sp;
    }

    private CatalogDraftResponseDto toResponseDto(StandardProduct sp) {
        Map<String, String> specs = null;
        if (sp.getSpecifications() != null && !sp.getSpecifications().isBlank()) {
            try {
                specs = objectMapper.readValue(sp.getSpecifications(),
                        new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {
                // specifications stored in non-JSON format from old API — return null
            }
        }

        return new CatalogDraftResponseDto(
                sp.getId(),
                sp.getDraftStep() != null ? sp.getDraftStep().name() : null,
                sp.getStatus() != null ? sp.getStatus().name() : null,
                sp.getName(),
                sp.getDescription(),
                sp.getCategory() != null ? sp.getCategory().getId() : null,
                sp.getCategory() != null ? sp.getCategory().getName() : null,
                sp.getEan(),
                sp.getProductCode(),
                specs,
                sp.getPrimaryImageUrl(),
                sp.getBrandEntity() != null ? sp.getBrandEntity().getId() : null,
                sp.getBrandEntity() != null ? sp.getBrandEntity().getName() : null,
                sp.getSearchKeywords());
    }
}
