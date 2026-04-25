package com.ProductClientService.ProductClientService.Service.seller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDocument;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.CategoryAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.CatalogSearchResultDto;
import com.ProductClientService.ProductClientService.DTO.seller.CreateListingFromCatalogDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductFullResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.DraftProductFullDto;
import com.ProductClientService.ProductClientService.DTO.seller.DraftProductFullDto.*;
import com.ProductClientService.ProductClientService.DTO.seller.ProductMediaResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductMedia;
import com.ProductClientService.ProductClientService.Model.ProductMedia.MediaType;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.Address;
import org.springframework.data.domain.PageRequest;
import com.ProductClientService.ProductClientService.Repository.AttributeRepository;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ProductMediaRepository;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService.AddressResponse;
import com.ProductClientService.ProductClientService.Service.ElasticsearchProductIndexer;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {
    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;
    private final ProductRepository productRepository;
    private final S3Service s3Service;
    private final CategoryRepository categoryRepository;
    private final HttpServletRequest request;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeRepository attributeRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final StandardProductRepository standardProductRepository;
    private final Cloudinary cloudinary;
    private final SellerRepository sellerRepository;
    private final BrandRepository brandRepository;
    private final OpenStreetMapService openStreetMapService;
    private final SellerAddressRepository sellerAddressRepository;
    private final EventPublisherService eventPublisher;
    private final ElasticsearchProductIndexer elasticsearchProductIndexer;
    private final ProductMediaRepository productMediaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public ApiResponse<Object> addProduct(ProductDto dto) {

        Product product;

        // =============================
        // UPDATE FLOW
        // =============================
        if (dto.productId() != null) {

            product = productRepository.findById(dto.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

        }
        // =============================
        // CREATE FLOW
        // =============================
        else {
            UUID sellerId = getUserId();
            boolean hasDraft = productRepository
                    .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(sellerId, Product.Step.LIVE)
                    .isPresent();
            if (hasDraft) {
                return new ApiResponse<>(false,
                        "You have an unfinished product. Please complete or discard it before creating a new one.",
                        null, 409);
            }
            product = new Product();
        }

        // Common fields
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setStep(Product.Step.valueOf(dto.step()));

        Seller seller = sellerRepository.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        product.setSeller(seller);
        if (dto.category() != null) {
            Category category = categoryRepository.findById(dto.category())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        UUID savedProductId = productRepository.save(product).getId();

        Map<String, Object> responseData = Map.of("productId", savedProductId);

        return new ApiResponse<>(
                true,
                dto.productId() == null ? "Product Created" : "Product Updated",
                responseData,
                200);
    }

    public ApiResponse<Object> discardDraftProduct() {

        UUID sellerId = (UUID) request.getAttribute("id");

        Optional<Product> draftProduct = productRepository
                .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(
                        sellerId,
                        Product.Step.LIVE);

        if (draftProduct.isEmpty()) {
            return new ApiResponse<>(false, "No Draft Product Found", null, 200);
        }

        productRepository.delete(draftProduct.get());

        return new ApiResponse<>(true, "Draft discarded successfully", null, 200);
    }

    @Transactional
    public ApiResponse<Object> getDraftProductFull() {
        try {
            UUID sellerId = getUserId();

            Product product = productRepository
                    .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(sellerId, Product.Step.LIVE)
                    .orElse(null);

            if (product == null) {
                return new ApiResponse<>(true, "No draft product found", null, 200);
            }

            UUID productId = product.getId();

            // ── Step 1: basic info ──────────────────────────────────────────────
            StepBasicInfo basicInfo = new StepBasicInfo(
                    product.getName(),
                    product.getDescription(),
                    product.getCategory() != null ? product.getCategory().getId() : null,
                    product.getCategory() != null ? product.getCategory().getName() : null);

            // ── Step 2: attributes ──────────────────────────────────────────────
            List<StepAttribute> attributes = product.getProductAttributes().stream()
                    .map(pa -> {
                        String attrName = pa.getCategoryAttribute() != null
                                && pa.getCategoryAttribute().getAttributes() != null
                                        ? pa.getCategoryAttribute().getAttributes().stream()
                                                .findFirst()
                                                .map(a -> a.getName())
                                                .orElse(null)
                                        : null;
                        boolean isImage = pa.getCategoryAttribute() != null
                                && Boolean.TRUE.equals(pa.getCategoryAttribute().getIsImageAttribute());
                        boolean isVariant = pa.getCategoryAttribute() != null
                                && Boolean.TRUE.equals(pa.getCategoryAttribute().getIsVariantAttribute());
                        return new StepAttribute(
                                pa.getId(),
                                pa.getCategoryAttribute() != null ? pa.getCategoryAttribute().getId() : null,
                                attrName,
                                pa.getValue(),
                                isImage,
                                isVariant,
                                pa.getImages() != null ? pa.getImages() : List.of());
                    })
                    .collect(Collectors.toList());

            // ── Step 3: variants ────────────────────────────────────────────────
            List<StepVariant> variants = product.getVariants().stream()
                    .map(v -> {
                        double price = 0, mrp = 0;
                        try {
                            price = Double.parseDouble(v.getPrice()) / 100;
                        } catch (Exception ignored) {
                        }
                        try {
                            mrp = Double.parseDouble(v.getMrp()) / 100;
                        } catch (Exception ignored) {
                        }
                        return new StepVariant(
                                v.getId(), v.getSku(), v.getLabel(),
                                price, mrp, v.getStock(), v.getCombination());
                    })
                    .collect(Collectors.toList());

            // ── Step 4: media ───────────────────────────────────────────────────
            List<ProductMedia> mediaList = productMediaRepository.findByProductIdOrderByPositionAsc(productId);
            String coverUrl = mediaList.stream()
                    .filter(ProductMedia::isCover)
                    .map(ProductMedia::getUrl)
                    .findFirst().orElse(null);

            // attribute media: attributeValue -> [urls] from ProductAttribute.images
            Map<String, List<String>> attrMedia = new java.util.LinkedHashMap<>();
            product.getProductAttributes().stream()
                    .filter(pa -> pa.getImages() != null && !pa.getImages().isEmpty())
                    .forEach(pa -> attrMedia.put(pa.getValue(), pa.getImages()));

            StepMedia media = new StepMedia(coverUrl, attrMedia.isEmpty() ? null : attrMedia);

            // ── Step 5: tags ────────────────────────────────────────────────────
            List<StepTag> tags = product.getTags().stream()
                    .map(t -> new StepTag(t.getId(), t.getName()))
                    .collect(Collectors.toList());

            // ── Step 6: brand ───────────────────────────────────────────────────
            StepBrand brand = product.getBrand() != null
                    ? new StepBrand(product.getBrand().getId(), product.getBrand().getName())
                    : null;

            DraftProductFullDto dto = new DraftProductFullDto(
                    productId,
                    product.getStep().name(),
                    basicInfo, attributes, variants, media, tags, brand);

            return new ApiResponse<>(true, "Draft product data fetched", dto, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to fetch draft: " + e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> loadAttribute(UUID id) {
        return new ApiResponse<>(true, "Step Completed", null, 200);
    }

    public ApiResponse<Object> getAttributesByCategoryId(UUID categoryId) {

        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository.findAllByCategoryId(categoryId);

        if (categoryAttributes.isEmpty()) {
            return new ApiResponse<>(false,
                    "No attributes found for this category",
                    null,
                    200);
        }

        List<AttributeDto> attributeDtos = categoryAttributes.stream()
                .flatMap(ca -> ca.getAttributes().stream()
                        .map(attr -> new AttributeDto(
                                ca.getId(), // ✅ categoryAttributeId
                                attr.getName(),
                                attr.getField_type(),
                                attr.getIs_required(),
                                attr.getOptions(),
                                attr.getIsRadio(),
                                ca.getIs_Required(),
                                ca.getIsImageAttribute(),
                                ca.getIsVariantAttribute(),
                                ca.getIsAdditionalAttribute())))
                .toList();

        CategoryAttributeDto dto = new CategoryAttributeDto(
                null,
                categoryId,
                attributeDtos);

        return new ApiResponse<>(true, "fetch data", dto, 200);
    }

    public ApiResponse<Object> addProductAttribute(ProductAttributeDto dto) {
        try {
            List<ProductAttributeResponseDto> productAttributeResponseDtos = saveAllAttributes(dto);
            return new ApiResponse<>(true, "Saved In The Db", productAttributeResponseDtos, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public List<ProductAttributeResponseDto> saveAllAttributes(ProductAttributeDto dto) {

        if (dto.categoryAttributeId() == null || dto.values() == null) {
            throw new RuntimeException("Invalid request");
        }

        if (dto.categoryAttributeId().size() != dto.values().size()) {
            throw new RuntimeException("Attribute and value size mismatch");
        }

        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStep(Product.Step.valueOf(dto.step()));

        // 💡 FIX: Add a flat index counter to iterate through the flat
        // productAttributeIds array
        int flatAttributeIdIndex = 0;

        for (int i = 0; i < dto.categoryAttributeId().size(); i++) {

            UUID categoryAttrId = dto.categoryAttributeId().get(i);
            List<String> vals = dto.values().get(i);

            if (vals == null || vals.isEmpty()) {
                continue;
            }

            CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(categoryAttrId)
                    .orElseThrow(() -> new RuntimeException("CategoryAttribute not found"));

            for (String value : vals) {
                UUID productAttributeId = null;

                // 💡 FIX: Grab the specific ID for THIS value using the flat counter, not 'i'
                if (dto.productAttributeIds() != null
                        && dto.productAttributeIds().size() > flatAttributeIdIndex
                        && dto.productAttributeIds().get(flatAttributeIdIndex) != null) {

                    productAttributeId = dto.productAttributeIds().get(flatAttributeIdIndex);
                }

                if (productAttributeId != null) {
                    // ✅ UPDATE existing entity
                    ProductAttribute existing = productAttributeRepository.findById(productAttributeId)
                            .orElseThrow(() -> new RuntimeException("ProductAttribute not found"));

                    existing.setValue(value);
                } else {
                    // ✅ CREATE new entity
                    ProductAttribute newAttribute = new ProductAttribute();
                    newAttribute.setProduct(product);
                    newAttribute.setCategoryAttribute(categoryAttribute);
                    newAttribute.setValue(value);

                    product.getProductAttributes().add(newAttribute);
                }

                // 💡 FIX: Increment flat index after processing each individual value
                flatAttributeIdIndex++;
            }
        }

        Product savedProduct = productRepository.save(product);

        return savedProduct.getProductAttributes()
                .stream()
                .map(pa -> {
                    CategoryAttribute ca = pa.getCategoryAttribute();

                    String attributeName = ca.getAttributes()
                            .stream()
                            .findFirst()
                            .map(Attribute::getName)
                            .orElse(null);

                    return new ProductAttributeResponseDto(
                            pa.getId(),
                            ca.getId(),
                            attributeName,
                            pa.getValue(),
                            List.of());
                })
                .toList();
    }
    // public ApiResponse<Object> getProductAttributes(UUID productId) {
    // try {
    // List<ProductAttribute> attributes =
    // productAttributeRepository.findByProductIdWithAttribute(productId);

    // // Map to DTO for clean response
    // List<Map<String, Object>> response = attributes.stream().map(attr -> {
    // Map<String, Object> map = new HashMap<>();
    // map.put("attributeId", attr.getCategory_attribute().getId());
    // map.put("attributeName",
    // attr.getCategory_attribute().getCategory().getName()); // you want name tooif
    // map.put("value", attr.getValue());
    // return map;
    // }).toList();

    // return new ApiResponse<>(true, "Fetched successfully", response, 200);
    // } catch (Exception e) {
    // return new ApiResponse<>(false, e.getMessage(), null, 500);
    // }
    // }

    public ApiResponse<Object> addProductVariants(ProductVariantsDto dto) {
        try {
            Product product = productRepository.findById(dto.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStep(Product.Step.PRODUCT_VARIANT);

            for (ProductVariantsDto.VariantItem item : dto.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setSku(item.sku());
                variant.setLabel(item.label());
                variant.setStock(item.stock());
                variant.setPrice(String.valueOf((long) (item.price() * 100)));
                variant.setMrp(String.valueOf((long) (item.mrp() * 100)));
                variant.setCombination(item.combination());
                variant = productVariantRepository.save(variant);
                product.getVariants().add(variant);
            }
            productRepository.save(product);

            return new ApiResponse<>(true, "Variants added successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> attachBrandToProduct(UUID productId, UUID brandId) {
        try {
            log.info("Attaching brand {} to product {}", brandId, productId);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new RuntimeException("Brand not found"));
            product.setBrand(brand);
            productRepository.save(product);
            return new ApiResponse<>(true, "Brand attached to product successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(),
                    null, 501);
        }
    }

    public ApiResponse<Object> handleLocation(SellerBasicInfo inforequest) {

        String phone = getUserPhone();

        System.out.println("calling OSM service "
                + inforequest.latitude().getClass()
                + inforequest.longitude().getClass());

        OpenStreetMapService.AddressResponse addressDetails = openStreetMapService.getAddressFromLatLng(
                inforequest.latitude(),
                inforequest.longitude());

        System.out.println("saving address");

        Address savedAddress = saveAddress(
                addressDetails,
                phone,
                inforequest.latitude(),
                inforequest.longitude());

        if (savedAddress == null) {
            return new ApiResponse<>(false, "Location Info Not Saved", null, 500);
        }

        return new ApiResponse<>(true, "Location Info Saved", savedAddress, 200);
    }

    private Address saveAddress(AddressResponse addressDetails, String phone, BigDecimal lat, BigDecimal longi) {

        Seller seller = sellerRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        if (seller.getOnboardingStage() == Seller.ONBOARDSTAGE.RESGISTER) {
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.LOCATION);
            sellerRepository.save(seller);
        }
        Address address = sellerAddressRepository.findBySellerId(seller.getId())
                .orElseGet(Address::new);

        System.out.println("City is " + addressDetails.city());

        address.setCity(addressDetails.city());
        address.setLine1(addressDetails.line1());
        address.setState(addressDetails.state());
        address.setCountry(addressDetails.country());
        address.setPincode(addressDetails.pincode());
        address.setLatitude(lat);
        address.setLongitude(longi);
        address.setSeller(seller);

        return sellerAddressRepository.save(address);
    }
    // public ApiResponse<Object> getProductWithAttributesAndVariants(UUID
    // productId) {
    // try {
    // Product product =
    // productRepository.findProductWithAttributesAndVariants(productId)
    // .orElseThrow(() -> new RuntimeException("Invalid productId: " + productId));
    // List<ProductAttributeResponseDto> attributesDto =
    // product.getProductAttributes().stream()
    // .map(attr -> new ProductAttributeResponseDto(
    // attr.getId(),
    // attr.getCategory_attribute().getCategory().getName(), // assuming you want
    // category name
    // attr.getValue(), // adjust if you store
    // attr.getVariants().stream()
    // .map(variant -> new ProductVariantResponseDto(
    // variant.getId(),
    // variant.getSku(),
    // variant.getPrice(),
    // variant.getStock()))
    // .toList()))
    // .toList();

    // ProductFullResponseDto responseDto = new ProductFullResponseDto(
    // product.getId(),
    // product.getName(),
    // product.getDescription(),
    // attributesDto);

    // return new ApiResponse<>(true, "Product details fetched successfully",
    // responseDto, 200);

    // } catch (Exception e) {
    // return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(),
    // null, 500);
    // }
    // }

    public ApiResponse<Object> MakeProductLive(UUID productId) {
        try {
            Product.Step currentStep = productRepository.findStepById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (currentStep == Product.Step.PRODUCT_BRAND_AND_TAGS
                    || currentStep == Product.Step.CATALOG_SELECTED) {
                int updatescore = updateStatusById(productId, Product.Step.LIVE);
                if (updatescore > 0) {
                    refreshSnapshot(productId);
                    handleProductUpdate(productId);
                    eventPublisher.publishProductLive(productId); // triggers search-intent indexing
                    elasticsearchProductIndexer.indexProduct(productId);
                    return new ApiResponse<>(true, "Product Live", null, 200);
                } else {
                    return new ApiResponse<>(false, "Internal Server Error", null, 500);
                }

            } else
                return new ApiResponse<>(false,
                        "Product is Not In  PRODUCT_BRAND_AND_TAGS or CATALOG_SELECTED , Current Step is "
                                + currentStep,
                        null, 403);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 500);
        }
    }

    private void refreshSnapshot(UUID productId) {
        String snapshot = productRepository.getProductDetailAsJson(productId);
        if (snapshot != null) {
            productRepository.updateSnapshot(productId, snapshot);
        }
    }

    private int updateStatusById(UUID productId, Product.Step step) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStep(step);
        productRepository.save(product); // 🔥 will trigger @PostUpdate
        return 1;
    }

    @Async
    public void handleProductUpdate(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (Boolean.TRUE.equals(product.getIsStandard()) && product.getStep() == Product.Step.LIVE) {
            // Product flagged as a new catalog candidate — requires admin review before
            // it can be added to the standard product catalog. No auto-creation here.
            log.info("Product {} is a new catalog candidate. Pending admin review for promotion.", productId);
        }
    }

    // ── Catalog listing flow
    // ──────────────────────────────────────────────────────

    public ApiResponse<Object> searchCatalog(String query, int page, int size) {
        List<StandardProduct> results = standardProductRepository
                .searchCatalog(query, PageRequest.of(page, size));
        List<CatalogSearchResultDto> dtos = results.stream().map(sp -> new CatalogSearchResultDto(
                sp.getId(),
                sp.getName(),
                sp.getDescription(),
                sp.getPrimaryImageUrl(),
                sp.getBrandEntity() != null ? sp.getBrandEntity().getName() : null,
                sp.getCategory() != null ? sp.getCategory().getName() : null,
                sp.getSpecifications(),
                sp.getEan(),
                sp.getProductCode())).toList();
        return new ApiResponse<>(true, "Catalog search results", dtos, 200);
    }

    @jakarta.transaction.Transactional
    public ApiResponse<Object> createListingFromCatalog(CreateListingFromCatalogDto dto) {
        StandardProduct std = standardProductRepository.findById(dto.standardProductId())
                .orElseThrow(() -> new RuntimeException("Standard product not found in catalog"));

        if (!Boolean.TRUE.equals(std.getIsVerified()) || std.getStatus() != StandardProduct.Status.ACTIVE) {
            return new ApiResponse<>(false, "This catalog entry is not available for listing", null, 400);
        }

        Seller seller = sellerRepository.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Product product = new Product();
        product.setName(std.getName());
        product.setDescription(std.getDescription());
        product.setCategory(std.getCategory());
        product.setBrand(std.getBrandEntity());
        product.setStandardProduct(std);
        product.setSeller(seller);
        // This is a seller listing linked to a catalog entry, not a new catalog item
        product.setIsStandard(false);
        // Skip attribute + image steps — inherited from standard product
        product.setStep(Product.Step.CATALOG_SELECTED);
        // Inherit the catalog's specifications snapshot so reads are zero-join
        product.setAttributesSnapshot(std.getSpecifications());

        UUID savedId = productRepository.save(product).getId();
        return new ApiResponse<>(true, "Listing created from catalog. Add variants to go live.",
                Map.of("productId", savedId), 201);
    }

    public ApiResponse<Object> uploadAndUpdateImages(List<Object[]> attributeImageData, String step) {
        try {
            System.out.println("Received attributeImageData: " + attributeImageData.size() + " entries");
            for (Object[] row : attributeImageData) {
                UUID productAttributeId = UUID.fromString(row[0].toString());
                List<MultipartFile> files = (List<MultipartFile>) row[1];

                // upload each image
                List<String> uploadedUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    try {
                        Map uploadResult = cloudinary.uploader()
                                .upload(file.getBytes(), ObjectUtils.emptyMap());
                        uploadedUrls.add(uploadResult.get("url").toString());
                    } catch (IOException e) {
                        return new ApiResponse<>(false,
                                "Failed to upload image for productAttributeId: " + productAttributeId,
                                null,
                                500);
                    }
                }

                // fetch product attribute
                ProductAttribute attribute = productAttributeRepository.findById(productAttributeId)
                        .orElseThrow(() -> new RuntimeException("ProductAttribute not found: " + productAttributeId));

                // update images list
                attribute.getImages().addAll(uploadedUrls);
                productAttributeRepository.save(attribute);
            }

            return new ApiResponse<>(true, "Images uploaded successfully", null, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Error while uploading images: " + e.getMessage(), null, 500);
        }
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm");

    @Transactional
    public ApiResponse<Object> uploadProductMedia(
            UUID productId,
            List<MultipartFile> coverFiles,
            List<String> attributeImageKeys,
            List<MultipartFile> attributeImages) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // ── 1. Cover / primary media ────────────────────────────────────────
            String coverUrl = null;
            if (coverFiles != null && !coverFiles.isEmpty()) {
                MultipartFile cover = coverFiles.get(0);
                String ct = cover.getContentType() != null ? cover.getContentType() : "";
                if (!ALLOWED_IMAGE_TYPES.contains(ct) && !ALLOWED_VIDEO_TYPES.contains(ct)) {
                    return new ApiResponse<>(false,
                            "Unsupported cover file type: " + ct, null, 400);
                }
                // idempotent: clear old cover media
                List<ProductMedia> existing = productMediaRepository.findByProductIdOrderByPositionAsc(productId);
                for (ProductMedia old : existing) {
                    try {
                        cloudinary.uploader().destroy(old.getPublicId(),
                                ObjectUtils.asMap("resource_type",
                                        old.getMediaType() == MediaType.VIDEO ? "video" : "image"));
                    } catch (Exception ignored) {
                    }
                }
                productMediaRepository.deleteAll(existing);

                boolean isVideo = ALLOWED_VIDEO_TYPES.contains(ct);
                Map uploadResult = cloudinary.uploader().upload(cover.getBytes(),
                        ObjectUtils.asMap(
                                "resource_type", isVideo ? "video" : "image",
                                "folder", "products/" + productId + "/cover"));

                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(uploadResult.get("secure_url").toString());
                media.setPublicId(uploadResult.get("public_id").toString());
                media.setMediaType(isVideo ? MediaType.VIDEO : MediaType.IMAGE);
                media.setPosition(0);
                media.setCover(true);
                productMediaRepository.save(media);
                coverUrl = media.getUrl();
            }

            // ── 2. Attribute images ─────────────────────────────────────────────
            // attributeImageKeys[i] = "{categoryAttributeId}::{value}"
            // attributeImages[i] = the image file for that key
            // Images are appended directly onto the matching ProductAttribute row.
            Map<String, List<String>> attributeMediaResult = new java.util.LinkedHashMap<>();

            if (attributeImageKeys != null && !attributeImageKeys.isEmpty()
                    && attributeImages != null && !attributeImages.isEmpty()) {

                if (attributeImageKeys.size() != attributeImages.size()) {
                    return new ApiResponse<>(false,
                            "Mismatch: attributeImageKeys count (" + attributeImageKeys.size()
                                    + ") != attributeImages count (" + attributeImages.size() + ")",
                            null, 400);
                }

                for (MultipartFile f : attributeImages) {
                    String ct = f.getContentType() != null ? f.getContentType() : "";
                    if (!ALLOWED_IMAGE_TYPES.contains(ct)) {
                        return new ApiResponse<>(false,
                                "Attribute images must be JPEG, PNG, or WEBP. Got: " + ct, null, 400);
                    }
                }

                // Idempotent: wipe existing Cloudinary assets + clear images/publicIds
                // on every ProductAttribute for this product that already has images.
                for (ProductAttribute pa : productAttributeRepository.findImageAttributesByProductId(productId)) {
                    if (pa.getImagePublicIds() != null) {
                        for (String pid : pa.getImagePublicIds()) {
                            try {
                                cloudinary.uploader().destroy(pid, ObjectUtils.emptyMap());
                            } catch (Exception ignored) {
                            }
                        }
                    }
                    pa.setImages(new ArrayList<>());
                    pa.setImagePublicIds(new ArrayList<>());
                    productAttributeRepository.save(pa);
                }

                for (int i = 0; i < attributeImageKeys.size(); i++) {
                    String key = attributeImageKeys.get(i);
                    String[] parts = key.split("::");
                    if (parts.length != 2) {
                        return new ApiResponse<>(false,
                                "Invalid key format: '" + key + "'. Expected '{categoryAttributeId}::{value}'",
                                null, 400);
                    }
                    UUID categoryAttributeId = UUID.fromString(parts[0].trim());
                    String attributeValue = parts[1].trim();

                    ProductAttribute pa = productAttributeRepository
                            .findByProductAndCategoryAttributeAndValue(productId, categoryAttributeId, attributeValue)
                            .orElseThrow(() -> new RuntimeException(
                                    "No ProductAttribute found for key: " + key));

                    Map uploadResult = cloudinary.uploader().upload(attributeImages.get(i).getBytes(),
                            ObjectUtils.asMap(
                                    "resource_type", "image",
                                    "folder", "products/" + productId + "/" + attributeValue));

                    pa.getImages().add(uploadResult.get("secure_url").toString());
                    pa.getImagePublicIds().add(uploadResult.get("public_id").toString());
                    productAttributeRepository.save(pa);

                    attributeMediaResult
                            .computeIfAbsent(attributeValue, v -> new ArrayList<>())
                            .add(uploadResult.get("secure_url").toString());
                }
            }

            product.setStep(Product.Step.PRODUCT_IMAGE);
            productRepository.save(product);

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("productId", productId.toString());
            if (coverUrl != null)
                data.put("coverImageUrl", coverUrl);
            if (!attributeMediaResult.isEmpty())
                data.put("attributeMedia", attributeMediaResult);

            return new ApiResponse<>(true, "Media uploaded successfully", data, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Upload failed: " + e.getMessage(), null, 500);
        }
    }

    @Transactional
    public ApiResponse<Object> removeProductMedia(UUID mediaId) {
        try {
            ProductMedia media = productMediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            UUID productId = media.getProduct().getId();
            boolean wasCover = media.isCover();

            cloudinary.uploader().destroy(
                    media.getPublicId(),
                    ObjectUtils.asMap("resource_type",
                            media.getMediaType() == MediaType.VIDEO ? "video" : "image"));

            productMediaRepository.delete(media);

            // auto-assign cover to first remaining image if the deleted one was cover
            if (wasCover) {
                productMediaRepository.findByProductIdOrderByPositionAsc(productId)
                        .stream()
                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
                        .findFirst()
                        .ifPresent(m -> {
                            m.setCover(true);
                            productMediaRepository.save(m);
                        });
            }

            return new ApiResponse<>(true, "Media removed", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Remove failed: " + e.getMessage(), null, 500);
        }
    }

    @Transactional
    public ApiResponse<Object> setCoverImage(UUID mediaId) {
        try {
            ProductMedia media = productMediaRepository.findById(mediaId)
                    .orElseThrow(() -> new RuntimeException("Media not found"));

            if (media.getMediaType() == MediaType.VIDEO) {
                return new ApiResponse<>(false, "Videos cannot be set as cover", null, 400);
            }

            productMediaRepository.clearCoverForProduct(media.getProduct().getId());
            media.setCover(true);
            productMediaRepository.save(media);

            return new ApiResponse<>(true, "Cover image updated", ProductMediaResponseDto.from(media), 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to set cover: " + e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> getProductMedia(UUID productId) {
        try {
            productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            List<ProductMediaResponseDto> result = productMediaRepository
                    .findByProductIdOrderByPositionAsc(productId)
                    .stream()
                    .map(ProductMediaResponseDto::from)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, "Media fetched", result, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> searchProducts(String keyword) throws IOException {
        SearchResponse<Map> response = elasticsearchClient.search(s -> s
                .index("products")
                .query(q -> q
                        .bool(b -> b
                                // name
                                .should(sh -> sh.match(m -> m
                                        .field("name")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("name.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // description
                                .should(sh -> sh.match(m -> m
                                        .field("description")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("description.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // sellerName
                                .should(sh -> sh.match(m -> m
                                        .field("sellerName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("sellerName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // categoryName
                                .should(sh -> sh.match(m -> m
                                        .field("categoryName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("categoryName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*")))

                                // brandName
                                .should(sh -> sh.match(m -> m
                                        .field("brandName")
                                        .query(keyword)
                                        .fuzziness("AUTO")))
                                .should(sh -> sh.wildcard(w -> w
                                        .field("brandName.keyword")
                                        .value("*" + keyword.toLowerCase() + "*"))))),
                Map.class);

        List<Map<String, Object>> result = response.hits().hits()
                .stream()
                .map(hit -> (Map<String, Object>) hit.source())
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "result", result, 200);
    }

    public ApiResponse<Object> getShopCategories() {
        try {
            // List<Seller.ShopCategory> categories =
            // sellerRepository.findAllShopCategories();
            return new ApiResponse<>(true, "Shop Categories fetched", "categories", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> searchShop(String keyword) {
        try {
            // List<Seller.ShopCategory> categories =
            // sellerRepository.findAllShopCategories();
            return new ApiResponse<>(true, "Shop Categories fetched", "categories", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> getShopsByCity(String city) {
        try {
            List<Seller> shops = sellerRepository.findByAddress_City(city);
            return new ApiResponse<>(true, "Shops fetched by city", shops, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    public ApiResponse<Object> getShopsByCityAndCategory(String city, Category category) {
        // List<Seller> shops = sellerRepository.findByAddress_CityAndShopCategory(city,
        // category);
        return new ApiResponse<>(true, "Shops fetched by city and category", "shops", 200);
    }

    public ApiResponse<Object> getNearestShops(double lat, double lon, int limit) {
        List<Seller> shops = sellerRepository.findNearestShops(lat, lon, limit);
        return new ApiResponse<>(true, "Nearest shops fetched", shops, 200);
    }

    public ApiResponse<Object> getNearestShopsByCategory(double lat, double lon, Category category,
            int limit) {
        try {
            // List<Seller> shops = sellerRepository.findNearestShopsByCategory(lat, lon,
            // category.name(), limit);
            return new ApiResponse<>(true, "Nearest shops by category fetched", "shops", 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 501);
        }
    }

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private String getUserPhone() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPhone();
    }
}
// hukiiu iuui jkjbhjhhjhj huhu uhh,j uh yiu ujhhuhjuhui uhh juyyuuik uhhu
// huiu8i iyu iy7uiu8u8uiui hujij juji uhj hij iji ijji