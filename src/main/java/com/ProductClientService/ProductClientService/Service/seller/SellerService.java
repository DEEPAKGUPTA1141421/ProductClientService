package com.ProductClientService.ProductClientService.Service.seller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import com.ProductClientService.ProductClientService.DTO.search.StandardCatalogDocument;
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
import com.ProductClientService.ProductClientService.Model.ProductMetrics;
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
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductMetricsRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService.AddressResponse;
import com.ProductClientService.ProductClientService.Service.BaseService;
import com.ProductClientService.ProductClientService.Service.ElasticsearchProductIndexer;
import com.ProductClientService.ProductClientService.Service.SearchResultsService;
import com.ProductClientService.ProductClientService.Service.SellerNotificationPublisher;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import com.ProductClientService.ProductClientService.DTO.search.SearchRequest;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse.SearchProductDto;
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
public class SellerService extends BaseService {
    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;
    // Defaults: 5MB images, 50MB videos — overridable in application.properties.
    @Value("${app.upload.max-image-bytes:5242880}")
    private long maxImageBytes;
    @Value("${app.upload.max-video-bytes:52428800}")
    private long maxVideoBytes;
    private final ProductRepository productRepository;
    private final SellerNotificationPublisher sellerNotificationPublisher;
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
    private final SearchResultsService searchResultsService;
    private final ProductRatingRepository productRatingRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final com.ProductClientService.ProductClientService.Repository.ReviewLikeRepository reviewLikeRepository;
    private final com.ProductClientService.ProductClientService.Service.ReviewService reviewService;
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

        // Apply isActive toggle on updates
        if (dto.productId() != null && dto.isActive() != null) {
            product.setIsActive(dto.isActive());
        }

        UUID savedProductId = productRepository.save(product).getId();

        // Re-index to ES whenever a LIVE product is updated
        if (product.getStep() == Product.Step.LIVE) CompletableFuture.runAsync(() -> elasticsearchProductIndexer.indexProduct(savedProductId));

        Map<String, Object> responseData = Map.of("productId", savedProductId);

        return new ApiResponse<>(
                true,
                dto.productId() == null ? "Product Created" : "Product Updated",
                responseData,
                200);
    }

    // ── GET /api/v1/seller/product/my-products ────────────────────────────────
    // Tries Elasticsearch first; falls back to a plain DB query if ES is down/unavailable.
    public ApiResponse<Object> getMyLiveProducts(
            int page, int size,
            String query, UUID categoryId, UUID brandId,
            Double minPrice, Double maxPrice,
            String sortBy, Boolean isActive, Integer maxStock) {

        UUID sellerId = getUserId();
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        try {
            return getMyLiveProductsFromEs(
                    sellerId, safePage, safeSize, query, categoryId, brandId,
                    minPrice, maxPrice, sortBy, isActive, maxStock);
        } catch (Exception e) {
            return getMyLiveProductsFromDb(sellerId, safePage, safeSize, isActive, maxStock);
        }
    }

    private ApiResponse<Object> getMyLiveProductsFromEs(
            UUID sellerId, int safePage, int safeSize,
            String query, UUID categoryId, UUID brandId,
            Double minPrice, Double maxPrice,
            String sortBy, Boolean isActive, Integer maxStock) throws java.io.IOException {

        SearchRequest req = new SearchRequest();
        req.setSellerId(sellerId);
        req.setPage(safePage);
        req.setPageSize(safeSize);
        if (query != null && !query.isBlank())
            req.setKeyword(query);
        if (categoryId != null)
            req.setCategoryId(categoryId);
        if (brandId != null)
            req.setBrandIds(List.of(brandId));
        if (minPrice != null)
            req.setMinPrice(minPrice);
        if (maxPrice != null)
            req.setMaxPrice(maxPrice);
        if (sortBy != null && !sortBy.isBlank())
            req.setSortBy(sortBy);

        SearchResultsResponse esResp = searchResultsService.searchStrict(req, sellerId);
        List<SearchProductDto> esDtos = esResp.getProducts() != null ? esResp.getProducts() : List.of();

        // Batch-load stock + isActive from DB for the returned product IDs
        List<UUID> ids = esDtos.stream()
                .map(SearchProductDto::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object[]> stockMap = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            productRepository.findStockAndActiveByIds(ids).forEach(row -> stockMap.put(row[0].toString(), row));
        }

        // Batch-load per-product engagement metrics (views/likes/sales), same
        // pattern as the stock/isActive batch load above.
        Map<UUID, ProductMetrics> metricsMap = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            productMetricsRepository.findAllById(ids).forEach(m -> metricsMap.put(m.getProductId(), m));
        }

        List<Map<String, Object>> products = esDtos.stream().map(dto -> {
            Map<String, Object> p = new java.util.LinkedHashMap<>();
            p.put("id", dto.getId() != null ? dto.getId().toString() : null);
            p.put("name", dto.getName());
            p.put("brand", dto.getBrand());
            p.put("categoryName", dto.getCategoryName());
            p.put("price", dto.getPrice());
            p.put("originalPrice", dto.getOriginalPrice());
            p.put("discountPercent", dto.getDiscountPercent());
            p.put("rating", dto.getRating());
            p.put("reviewCount", dto.getReviewCount());
            p.put("imageUrl", dto.getImages() != null && !dto.getImages().isEmpty()
                    ? dto.getImages().get(0)
                    : null);
            p.put("variantId", dto.getVariantId() != null ? dto.getVariantId().toString() : null);
            Object[] stockRow = dto.getId() != null ? stockMap.get(dto.getId().toString()) : null;
            int stockVal = stockRow != null && stockRow[1] != null ? ((Number) stockRow[1]).intValue() : 0;
            boolean activeVal = stockRow != null && stockRow[2] != null ? (Boolean) stockRow[2] : true;
            p.put("stock", stockVal);
            p.put("isActive", activeVal);
            ProductMetrics metrics = dto.getId() != null ? metricsMap.get(dto.getId()) : null;
            p.put("views", metrics != null ? metrics.getViewCount() : 0L);
            p.put("likes", metrics != null ? metrics.getWishlistCount() : 0L);
            p.put("sales", metrics != null ? metrics.getNumberOfPurchases() : 0L);
            return p;
        })
                // Post-ES filters applied on DB values (isActive, maxStock)
                .filter(p -> isActive == null || isActive.equals(p.get("isActive")))
                .filter(p -> maxStock == null || ((Number) p.get("stock")).intValue() <= maxStock)
                .collect(Collectors.toList());

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("products", products);
        payload.put("totalCount", esResp.getTotalCount());
        payload.put("hasMore", esResp.isHasMore());
        payload.put("source", "elasticsearch");
        return new ApiResponse<>(true, "Products fetched", payload, 200);
    }

    // ── GET /api/v1/seller/product/my-categories ──────────────────────────────
    // Distinct categories the seller actually has LIVE products in (e.g. a seller
    // who's listed Shirts, T-shirts and Jeans only sees those three) — used to
    // populate the category filter on the seller's product list, instead of the
    // full global category tree.
    public ApiResponse<Object> getMyProductCategories() {
        UUID sellerId = getUserId();
        List<Object[]> rows = productRepository.findDistinctCategoriesBySeller(sellerId);
        List<Map<String, Object>> categories = rows.stream().map(row -> {
            Map<String, Object> c = new java.util.LinkedHashMap<>();
            c.put("id", row[0] != null ? row[0].toString() : null);
            c.put("name", row[1]);
            return c;
        }).collect(Collectors.toList());
        return new ApiResponse<>(true, "Categories fetched", categories, 200);
    }

    /** DB fallback used when Elasticsearch is down/unavailable. Only supports pagination + isActive/maxStock filters. */
    private ApiResponse<Object> getMyLiveProductsFromDb(
            UUID sellerId, int safePage, int safeSize, Boolean isActive, Integer maxStock) {
        int offset = safePage * safeSize;

        List<Object[]> rows = productRepository.findLiveProductsBySeller(sellerId, safeSize, offset);
        long totalCount = productRepository.countLiveProductsBySeller(sellerId);

        // Batch-load per-product engagement metrics (views/likes/sales), same
        // pattern used by the ES-backed variant above.
        List<UUID> ids = rows.stream()
                .map(row -> row[0] != null ? UUID.fromString(row[0].toString()) : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        Map<UUID, ProductMetrics> metricsMap = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            productMetricsRepository.findAllById(ids).forEach(m -> metricsMap.put(m.getProductId(), m));
        }

        List<Map<String, Object>> products = rows.stream().map(row -> {
            Map<String, Object> p = new java.util.LinkedHashMap<>();
            p.put("id", row[0] != null ? row[0].toString() : null);
            p.put("name", row[1]);
            p.put("brand", null);
            p.put("categoryName", null);
            p.put("price", row[5]); // min variant price
            p.put("originalPrice", null);
            p.put("discountPercent", null);
            p.put("rating", null);
            p.put("reviewCount", null);
            p.put("imageUrl", row[7]); // cover image URL (may be null)
            p.put("variantId", null);
            p.put("stock", row[6]); // total stock across variants
            p.put("isActive", row[8] == null || (Boolean) row[8]);
            ProductMetrics metrics = row[0] != null ? metricsMap.get(UUID.fromString(row[0].toString())) : null;
            p.put("views", metrics != null ? metrics.getViewCount() : 0L);
            p.put("likes", metrics != null ? metrics.getWishlistCount() : 0L);
            p.put("sales", metrics != null ? metrics.getNumberOfPurchases() : 0L);
            return p;
        })
                .filter(p -> isActive == null || isActive.equals(p.get("isActive")))
                .filter(p -> maxStock == null || ((Number) p.get("stock")).intValue() <= maxStock)
                .collect(Collectors.toList());

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("products", products);
        payload.put("totalCount", totalCount);
        payload.put("hasMore", (long) (offset + rows.size()) < totalCount);
        payload.put("source", "database");
        return new ApiResponse<>(true, "Products fetched", payload, 200);
    }

    // ── GET /api/v1/seller/product/scheduled-products ──────────────────────────
    // Products the seller finished building but chose to publish later instead
    // of going live immediately (see scheduleProduct below).
    public ApiResponse<Object> getScheduledProducts(int page, int size, String query) {
        UUID sellerId = getUserId();
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        List<Object[]> rows = productRepository.findScheduledProductsBySeller(sellerId, safeQuery, safeSize, offset);
        long totalCount = productRepository.countScheduledProductsBySeller(sellerId, safeQuery);

        List<Map<String, Object>> products = rows.stream().map(row -> {
            Map<String, Object> p = new java.util.LinkedHashMap<>();
            p.put("id", row[0] != null ? row[0].toString() : null);
            p.put("name", row[1]);
            p.put("scheduledAt", row[3]);
            p.put("lastEdited", row[4]);
            p.put("price", row[5]);
            p.put("imageUrl", row[6]);
            return p;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("products", products);
        payload.put("totalCount", totalCount);
        payload.put("hasMore", (long) (offset + rows.size()) < totalCount);
        return new ApiResponse<>(true, "Scheduled products fetched", payload, 200);
    }

    // ── POST /api/v1/seller/product/{productId}/schedule ───────────────────────
    // Sets (or updates — "Reschedule") the future auto-publish time for a
    // product that has finished the listing wizard but isn't live yet. The
    // ScheduledProductPublishJob cron picks it up once scheduledAt arrives.
    @Transactional
    public ApiResponse<Object> scheduleProduct(UUID productId, java.time.ZonedDateTime scheduledAt) {
        UUID sellerId = getUserId();
        if (scheduledAt == null || !scheduledAt.isAfter(java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")))) {
            return new ApiResponse<>(false, "scheduledAt must be a future date/time", null, 400);
        }
        Optional<Product.Step> stepOpt = productRepository.findStepByIdAndSellerId(productId, sellerId);
        if (stepOpt.isEmpty()) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        Product.Step step = stepOpt.get();
        if (step != Product.Step.PRODUCT_BRAND_AND_TAGS && step != Product.Step.CATALOG_SELECTED) {
            return new ApiResponse<>(false,
                    "Product is not ready to be scheduled, current step is " + step, null, 409);
        }
        productRepository.updateScheduledAtByIdAndSellerId(productId, sellerId, scheduledAt);
        return new ApiResponse<>(true, "Product scheduled", Map.of("scheduledAt", scheduledAt.toString()), 200);
    }

    // ── POST /api/v1/seller/product/{productId}/publish-now ────────────────────
    // Publishes a scheduled product immediately instead of waiting for its
    // scheduledAt — reuses MakeProductLive, which also clears scheduledAt.
    public ApiResponse<Object> publishScheduledProductNow(UUID productId) {
        UUID sellerId = getUserId();
        UUID ownerId = productRepository.findSellerIdByProductId(productId);
        if (ownerId == null) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        if (!ownerId.equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        return MakeProductLive(productId);
    }

    // ── DELETE /api/v1/seller/product/{productId} ─────────────────────────────
    @Transactional
    public ApiResponse<Object> deleteMyProduct(UUID productId) {
        UUID sellerId = getUserId();
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        Product product = opt.get();
        if (!product.getSeller().getId().equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        try {
            productRepository.delete(product);
            return new ApiResponse<>(true, "Product deleted", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Cannot delete — product may have associated orders", null, 409);
        }
    }

    // ── PATCH /api/v1/seller/product/{productId}/quick-update ────────────────
    @Transactional
    public ApiResponse<Object> quickUpdate(UUID productId, String name, Long priceInPaise, Integer stock) {
        UUID sellerId = getUserId();
        Optional<String> currentName = productRepository.findNameByIdAndSellerId(productId, sellerId);
        if (currentName.isEmpty()) {
            UUID ownerId = productRepository.findSellerIdByProductId(productId);
            if (ownerId != null) {
                return new ApiResponse<>(false, "Access denied", null, 403);
            }
            return new ApiResponse<>(false, "Product not found", null, 404);
        }

        String responseName = currentName.get();
        if (name != null && !name.isBlank()) {
            responseName = name.trim();
            productRepository.updateNameByIdAndSellerId(productId, sellerId, responseName);
        }
        if (priceInPaise != null || stock != null) {
            productVariantRepository.updateAllByProductId(productId, priceInPaise, stock);
            if (stock != null) {
                checkLowStockAndNotify(productId, sellerId);
            }
        }
        return new ApiResponse<>(true, "Product updated", java.util.Map.of(
                "id", productId.toString(),
                "name", responseName), 200);
    }

    // ── PATCH /api/v1/seller/product/{productId}/toggle-active ───────────────
    @Transactional
    public ApiResponse<Object> toggleActive(UUID productId) {
        UUID sellerId = getUserId();
        Optional<Boolean> currentActive = productRepository.findActiveByIdAndSellerId(productId, sellerId);
        if (currentActive.isEmpty()) {
            UUID ownerId = productRepository.findSellerIdByProductId(productId);
            if (ownerId != null) {
                return new ApiResponse<>(false, "Access denied", null, 403);
            }
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        boolean newActive = !Boolean.TRUE.equals(currentActive.get());
        productRepository.updateActiveByIdAndSellerId(productId, sellerId, newActive);
        return new ApiResponse<>(true,
                newActive ? "Product activated" : "Product deactivated",
                java.util.Map.of("isActive", newActive), 200);
    }

    public ApiResponse<Object> discardDraftProduct() {
        Optional<Product> draftProduct = productRepository
                .findTopBySellerIdAndStepNotOrderByCreatedAtDesc(
                        getUserId(),
                        Product.Step.LIVE);

        if (draftProduct.isEmpty()) {
            return new ApiResponse<>(false, "No Draft Product Found", null, 200);
        }

        productRepository.deleteById(draftProduct.get().getId());

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

        // Re-index to ES if product is already LIVE
        if (savedProduct.getStep() == Product.Step.LIVE) {
            try {
                elasticsearchProductIndexer.indexProduct(savedProduct.getId());
            } catch (Exception e) {
                log.warn("ES re-index failed: {}", e.getMessage());
            }
        }

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
        // Validate any discount config up front so a bad discount doesn't 500 —
        // same validation rules as configureVariantDiscount.
        for (ProductVariantsDto.VariantItem item : dto.variants()) {
            com.ProductClientService.ProductClientService.DTO.seller.VariantDiscountDto d = item.discount();
            if (d == null) continue;
            if (d.type() == com.ProductClientService.ProductClientService.Model.DiscountType.PERCENTAGE
                    && d.value() > 90) {
                return new ApiResponse<>(false, "Percentage discount cannot exceed 90%", null, 400);
            }
            if (d.startsAt() != null && d.endsAt() != null && !d.startsAt().isBefore(d.endsAt())) {
                return new ApiResponse<>(false, "startsAt must be before endsAt", null, 400);
            }
        }

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
                if (item.discount() != null) {
                    com.ProductClientService.ProductClientService.DTO.seller.VariantDiscountDto d = item.discount();
                    variant.setDiscountType(d.type());
                    variant.setDiscountValue(String.valueOf(d.value()));
                    variant.setDiscountActive(d.active());
                    variant.setDiscountStartsAt(d.startsAt());
                    variant.setDiscountEndsAt(d.endsAt());
                    variant.recomputeEffectiveDiscount();
                }
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
            if (product.getStep() != Product.Step.LIVE) {
                product.setStep(Product.Step.PRODUCT_BRAND_AND_TAGS);
            }
            productRepository.save(product);
            if (product.getStep() == Product.Step.LIVE) CompletableFuture.runAsync(() -> elasticsearchProductIndexer.indexProduct(productId));
            return new ApiResponse<>(true, "Brand attached to product successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(),
                    null, 501);
        }
    }

    public ApiResponse<Object> handleLocation(SellerBasicInfo inforequest) {

        OpenStreetMapService.AddressResponse addressDetails = openStreetMapService.getAddressFromLatLng(
                inforequest.latitude(),
                inforequest.longitude());

        Address savedAddress = saveAddress(
                addressDetails,
                inforequest.latitude(),
                inforequest.longitude());

        if (savedAddress == null) {
            return new ApiResponse<>(false, "Location Info Not Saved", null, 500);
        }
        return new ApiResponse<>(true, "Location Info Saved", savedAddress, 200);
    }

    private Address saveAddress(AddressResponse addressDetails, BigDecimal lat, BigDecimal longi) {

        Seller seller = sellerRepository.findById(getUserId())
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
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            Product.Step currentStep = product.getStep();
            if (currentStep != Product.Step.PRODUCT_BRAND_AND_TAGS
                    && currentStep != Product.Step.CATALOG_SELECTED) {
                return new ApiResponse<>(false,
                        "Product is Not In  PRODUCT_BRAND_AND_TAGS or CATALOG_SELECTED , Current Step is "
                                + currentStep,
                        null, 409);
            }

            product.setStep(Product.Step.LIVE);
            product.setScheduledAt(null);
            productRepository.save(product); // 🔥 will trigger @PostUpdate

            CompletableFuture.runAsync(() -> {
                refreshSnapshot(productId);
                handleProductUpdate(productId, product.getIsStandard());
                eventPublisher.publishProductLive(productId);
                elasticsearchProductIndexer.indexProduct(productId);
            }).exceptionally(ex -> {
                log.error("Async product update failed for productId={}", productId, ex);
                return null;
            });

            return new ApiResponse<>(true, "Product Live", null, 200);
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

    public void handleProductUpdate(UUID productId, Boolean isStandard) {
        if (Boolean.TRUE.equals(isStandard)) {
            // Product flagged as a new catalog candidate — requires admin review before
            // it can be added to the standard product catalog. No auto-creation here.
            log.info("Product {} is a new catalog candidate. Pending admin review for promotion.", productId);
        }
    }

    // ── Catalog listing flow
    // ──────────────────────────────────────────────────────

    @org.springframework.cache.annotation.Cacheable(value = "catalog-search", key = "#query + ':' + #page + ':' + #size")
    public ApiResponse<Object> searchCatalog(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return new ApiResponse<>(false, "Query cannot be empty", null, 400);
        }
        // Strip SQL wildcard characters — safety for the DB fallback path
        String sanitized = query.trim().replaceAll("[%_\\\\]", "");
        sanitized = sanitized.substring(0, Math.min(sanitized.length(), 100));
        if (sanitized.length() < 2) {
            return new ApiResponse<>(false, "Search query must be at least 2 characters", null, 400);
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));

        try {
            List<CatalogSearchResultDto> dtos = searchCatalogInEs(sanitized, safePage, safeSize);
            return new ApiResponse<>(true, "Catalog search results", dtos, 200);
        } catch (Exception e) {
            log.warn("Catalog ES search failed, falling back to DB query: {}", e.getMessage());
            return searchCatalogFromDb(sanitized, safePage, safeSize);
        }
    }

    /**
     * Queries the catalog-v1 Elasticsearch index.
     *
     * Query strategy:
     * filter: is_verified=true AND status=ACTIVE (cached, no scoring)
     * must: inner bool with minimumShouldMatch=1
     * should[0] multi_match on name^3 / search_keywords^2 / brand_name^2
     * / description / category_name — fuzziness AUTO
     * should[1] exact term match on ean (barcode scan)
     * should[2] exact term match on product_code (uppercase)
     */
    private List<CatalogSearchResultDto> searchCatalogInEs(String query, int page, int size)
            throws java.io.IOException {
        final String q = query;
        int from = page * size;

        co.elastic.clients.elasticsearch.core.SearchResponse<StandardCatalogDocument> response = elasticsearchClient
                .search(s -> s
                        .index("catalog-v1")
                        .from(from)
                        .size(size)
                        .query(outer -> outer.bool(b -> b
                                // ── fast cached pre-filters ─────────────────────────────
                                .filter(f -> f.term(t -> t
                                        .field("is_verified")
                                        .value(v -> v.booleanValue(true))))
                                .filter(f -> f.term(t -> t
                                        .field("status")
                                        .value("ACTIVE")))
                                // ── relevance: at least one clause must match ────────────
                                .must(m -> m.bool(inner -> inner
                                        .minimumShouldMatch("1")
                                        .should(sh -> sh.multiMatch(mm -> mm
                                                .query(q)
                                                .fields("name^3", "search_keywords^2",
                                                        "brand_name^2", "description",
                                                        "category_name")
                                                .fuzziness("AUTO")
                                                .prefixLength(1)))
                                        .should(sh -> sh.term(t -> t
                                                .field("ean")
                                                .value(q)))
                                        .should(sh -> sh.term(t -> t
                                                .field("product_code")
                                                .value(q.toUpperCase()))))))),
                        StandardCatalogDocument.class);

        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .filter(doc -> doc != null && doc.getCatalogId() != null)
                .map(doc -> new CatalogSearchResultDto(
                        UUID.fromString(doc.getCatalogId()),
                        doc.getName(),
                        doc.getDescription(),
                        doc.getPrimaryImageUrl(),
                        doc.getBrandName(),
                        doc.getCategoryName(),
                        doc.getSpecifications(),
                        doc.getEan(),
                        doc.getProductCode()))
                .collect(Collectors.toList());
    }

    /** DB fallback used when Elasticsearch is unavailable. */
    private ApiResponse<Object> searchCatalogFromDb(String query, int page, int size) {
        List<StandardProduct> results = standardProductRepository
                .searchCatalog(query, PageRequest.of(page, size));
        List<CatalogSearchResultDto> dtos = results.stream()
                .map(sp -> new CatalogSearchResultDto(
                        sp.getId(), sp.getName(), sp.getDescription(),
                        sp.getPrimaryImageUrl(),
                        sp.getBrandEntity() != null ? sp.getBrandEntity().getName() : null,
                        sp.getCategory() != null ? sp.getCategory().getName() : null,
                        sp.getSpecifications(), sp.getEan(), sp.getProductCode()))
                .toList();
        return new ApiResponse<>(true, "Catalog search results", dtos, 200);
    }

    // Returns a single verified+active catalog entry for the detail view
    public ApiResponse<Object> getCatalogDetail(UUID standardProductId) {
        return standardProductRepository.findById(standardProductId)
                .filter(sp -> Boolean.TRUE.equals(sp.getIsVerified())
                        && sp.getStatus() == StandardProduct.Status.ACTIVE)
                .map(sp -> new ApiResponse<Object>(true, "Catalog item found",
                        new CatalogSearchResultDto(
                                sp.getId(), sp.getName(), sp.getDescription(),
                                sp.getPrimaryImageUrl(),
                                sp.getBrandEntity() != null ? sp.getBrandEntity().getName() : null,
                                sp.getCategory() != null ? sp.getCategory().getName() : null,
                                sp.getSpecifications(), sp.getEan(), sp.getProductCode()),
                        200))
                .orElse(new ApiResponse<>(false, "Catalog item not found or unavailable", null, 404));
    }

    @Transactional
    public ApiResponse<Object> createListingFromCatalog(CreateListingFromCatalogDto dto) {
        StandardProduct std = standardProductRepository.findById(dto.standardProductId())
                .orElseThrow(() -> new RuntimeException("Standard product not found in catalog"));

        if (!Boolean.TRUE.equals(std.getIsVerified()) || std.getStatus() != StandardProduct.Status.ACTIVE) {
            return new ApiResponse<>(false, "This catalog entry is not available for listing", null, 400);
        }

        Seller seller = sellerRepository.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (productRepository.findBySellerIdAndStandardProductId(seller.getId(), std.getId()).isPresent()) {
            return new ApiResponse<>(false, "This standard product is already in your catalog", null, 409);
        }

        Product product = new Product();
        product.setName(std.getName());
        product.setDescription(std.getDescription());
        product.setCategory(std.getCategory());
        product.setBrand(std.getBrandEntity());
        product.setStandardProduct(std);
        product.setSeller(seller);
        product.setIsStandard(false);
        product.setStep(Product.Step.CATALOG_SELECTED);
        product.setAttributesSnapshot(std.getSpecifications());

        product = productRepository.save(product);
        final UUID savedId = product.getId();

        // Persist all variants in one transaction
        int idx = 0;
        for (CreateListingFromCatalogDto.VariantItem item : dto.variants()) {
            String sku = (item.sku() != null && !item.sku().isBlank())
                    ? item.sku().trim()
                    : (std.getProductCode() != null ? std.getProductCode() : "SKU") + "-" + (++idx);

            ProductVariant variant = new ProductVariant();
            variant.setSku(sku);
            variant.setLabel(item.label() != null ? item.label().trim() : "");
            variant.setStock(item.stock());
            variant.setPrice(String.valueOf((long) (item.price() * 100)));
            double effectiveMrp = item.mrp() >= item.price() ? item.mrp() : item.price();
            variant.setMrp(String.valueOf((long) (effectiveMrp * 100)));
            variant.setCombination(item.combination() != null ? item.combination() : Map.of());
            variant = productVariantRepository.save(variant);
            product.getVariants().add(variant);
        }
        productRepository.save(product);

        // Go live immediately when requested and variants are present
        if (dto.goLive() && !dto.variants().isEmpty()) {
            product.setStep(Product.Step.LIVE);
            product.setIsActive(true);
            productRepository.save(product);
            CompletableFuture.runAsync(() -> {
                refreshSnapshot(savedId);
                eventPublisher.publishProductLive(savedId);
                elasticsearchProductIndexer.indexProduct(savedId);
            });

        }

        String step = product.getStep().name();
        String imageUrl = std.getPrimaryImageUrl() != null ? std.getPrimaryImageUrl() : "";
        int variantCount = dto.variants().size();

        return new ApiResponse<>(true,
                dto.goLive() ? "Product listed successfully!" : "Listing created. Add variants to go live.",
                Map.of("productId", savedId, "name", std.getName(),
                        "imageUrl", imageUrl, "step", step, "variantCount", variantCount),
                201);
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

                // Additive: each uploaded image is appended to its attribute value's
                // existing gallery rather than wiping the product's other attribute
                // images first — mirrors add-variants' append-only semantics so
                // editing an existing product can safely add just the new photos.
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

            // Don't regress an already-published product back to an earlier wizard
            // step — this endpoint is also used to update a LIVE product's photos
            // from the edit flow.
            if (product.getStep() != Product.Step.LIVE) {
                product.setStep(Product.Step.PRODUCT_IMAGE);
            }
            productRepository.save(product);

            if (product.getStep() == Product.Step.LIVE) {
                CompletableFuture.runAsync(() -> elasticsearchProductIndexer.indexProduct(productId));
            }

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

    // ── Presigned direct-to-Cloudinary upload ───────────────────────────────
    // The app no longer proxies raw photo/video bytes through this server —
    // it asks here for a short-lived signature, uploads straight to
    // Cloudinary itself, then calls confirmMediaUpload() with just the
    // resulting metadata. Keeps this server's bandwidth/memory out of the
    // upload path entirely while still enforcing size limits and ownership.

    public ApiResponse<Object> createMediaUploadSignature(UUID productId,
            com.ProductClientService.ProductClientService.DTO.seller.MediaSignatureRequestDto req) {
        try {
            UUID sellerId = getUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getSeller().getId().equals(sellerId)) {
                return new ApiResponse<>(false, "Not authorized for this product", null, 403);
            }

            String resourceType = req.resourceType().toLowerCase();
            String folder;
            if ("attribute".equals(req.purpose())) {
                String key = req.attributeKey();
                if (key == null || !key.contains("::")) {
                    return new ApiResponse<>(false,
                            "Valid attributeKey ('{categoryAttributeId}::{value}') is required", null, 400);
                }
                String attributeValue = key.substring(key.indexOf("::") + 2).trim();
                folder = "products/" + productId + "/" + attributeValue;
            } else {
                folder = "products/" + productId + "/cover";
            }

            String publicId = UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis() / 1000;

            // Only params the client is allowed to send are signed — folder and
            // public_id are pinned here, so a tampered upload request (different
            // folder, different public_id) will simply fail Cloudinary's own
            // signature check rather than silently landing somewhere else.
            Map<String, Object> paramsToSign = new java.util.TreeMap<>();
            paramsToSign.put("folder", folder);
            paramsToSign.put("public_id", publicId);
            paramsToSign.put("timestamp", timestamp);
            String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

            var dto = new com.ProductClientService.ProductClientService.DTO.seller.MediaSignatureResponseDto(
                    cloudinary.config.cloudName,
                    cloudinary.config.apiKey,
                    signature,
                    timestamp,
                    folder,
                    publicId,
                    resourceType,
                    "video".equals(resourceType) ? maxVideoBytes : maxImageBytes);

            return new ApiResponse<>(true, "Signature generated", dto, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Could not generate upload signature: " + e.getMessage(), null, 500);
        }
    }

    @Transactional
    public ApiResponse<Object> confirmMediaUpload(
            com.ProductClientService.ProductClientService.DTO.seller.MediaConfirmRequestDto req) {
        try {
            UUID sellerId = getUserId();
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getSeller().getId().equals(sellerId)) {
                return new ApiResponse<>(false, "Not authorized for this product", null, 403);
            }

            boolean isVideo = "video".equals(req.resourceType().toLowerCase());
            long limit = isVideo ? maxVideoBytes : maxImageBytes;

            // Defense in depth: the client already checked size before
            // uploading, but a modified client could skip that check — this
            // re-validates the ACTUAL bytes Cloudinary reports (not anything
            // the client claims) and destroys the asset if it's oversized
            // instead of trusting the upload was legitimate.
            if (req.bytes() > limit) {
                try {
                    cloudinary.uploader().destroy(req.publicId(),
                            ObjectUtils.asMap("resource_type", isVideo ? "video" : "image"));
                } catch (Exception ignored) {
                }
                return new ApiResponse<>(false,
                        (isVideo ? "Video" : "Image") + " exceeds the " + (limit / (1024 * 1024)) + "MB limit",
                        null, 400);
            }

            // The uploaded asset's own folder must match this product — guards
            // against confirming an asset that was signed for a different
            // product/seller (e.g. a stale signature reused after the fact).
            String expectedFolderPrefix = "products/" + req.productId() + "/";
            if (!req.publicId().contains(expectedFolderPrefix) && !req.secureUrl().contains(expectedFolderPrefix)) {
                return new ApiResponse<>(false, "Uploaded asset does not belong to this product", null, 400);
            }

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("productId", product.getId().toString());

            if ("attribute".equals(req.purpose())) {
                String key = req.attributeKey();
                if (key == null || !key.contains("::")) {
                    return new ApiResponse<>(false, "Valid attributeKey is required", null, 400);
                }
                String[] parts = key.split("::", 2);
                UUID categoryAttributeId = UUID.fromString(parts[0].trim());
                String attributeValue = parts[1].trim();

                ProductAttribute pa = productAttributeRepository
                        .findByProductAndCategoryAttributeAndValue(product.getId(), categoryAttributeId,
                                attributeValue)
                        .orElseThrow(() -> new RuntimeException("No ProductAttribute found for key: " + key));

                pa.getImages().add(req.secureUrl());
                pa.getImagePublicIds().add(req.publicId());
                productAttributeRepository.save(pa);

                data.put("attributeMedia", Map.of(attributeValue, List.of(req.secureUrl())));
            } else {
                // Cover media — idempotent: replace whatever was there before,
                // same semantics the old multipart flow had.
                List<ProductMedia> existing = productMediaRepository.findByProductIdOrderByPositionAsc(product.getId());
                for (ProductMedia old : existing) {
                    try {
                        cloudinary.uploader().destroy(old.getPublicId(),
                                ObjectUtils.asMap("resource_type",
                                        old.getMediaType() == MediaType.VIDEO ? "video" : "image"));
                    } catch (Exception ignored) {
                    }
                }
                productMediaRepository.deleteAll(existing);

                ProductMedia media = new ProductMedia();
                media.setProduct(product);
                media.setUrl(req.secureUrl());
                media.setPublicId(req.publicId());
                media.setMediaType(isVideo ? MediaType.VIDEO : MediaType.IMAGE);
                media.setPosition(0);
                media.setCover(true);
                productMediaRepository.save(media);

                data.put("coverImageUrl", req.secureUrl());
            }

            if (product.getStep() != Product.Step.LIVE) {
                product.setStep(Product.Step.PRODUCT_IMAGE);
            }
            productRepository.save(product);

            if (product.getStep() == Product.Step.LIVE) {
                CompletableFuture.runAsync(() -> elasticsearchProductIndexer.indexProduct(product.getId()));
            }

            return new ApiResponse<>(true, "Media attached successfully", data, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Could not attach media: " + e.getMessage(), null, 500);
        }
    }

    /** Deletes one already-confirmed media item: the DB reference AND the Cloudinary asset itself. */
    @Transactional
    public ApiResponse<Object> removeConfirmedMedia(
            com.ProductClientService.ProductClientService.DTO.seller.MediaRemoveRequestDto req) {
        try {
            UUID sellerId = getUserId();
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getSeller().getId().equals(sellerId)) {
                return new ApiResponse<>(false, "Not authorized for this product", null, 403);
            }

            if ("attribute".equals(req.purpose())) {
                String key = req.attributeKey();
                if (key == null || !key.contains("::")) {
                    return new ApiResponse<>(false, "Valid attributeKey is required", null, 400);
                }
                String[] parts = key.split("::", 2);
                UUID categoryAttributeId = UUID.fromString(parts[0].trim());
                String attributeValue = parts[1].trim();

                ProductAttribute pa = productAttributeRepository
                        .findByProductAndCategoryAttributeAndValue(req.productId(), categoryAttributeId,
                                attributeValue)
                        .orElseThrow(() -> new RuntimeException("No ProductAttribute found for key: " + key));

                int idx = pa.getImages().indexOf(req.url());
                if (idx == -1) {
                    return new ApiResponse<>(false, "Image not found on this attribute", null, 404);
                }
                String publicId = idx < pa.getImagePublicIds().size() ? pa.getImagePublicIds().get(idx) : null;
                pa.getImages().remove(idx);
                if (publicId != null) {
                    if (idx < pa.getImagePublicIds().size()) {
                        pa.getImagePublicIds().remove(idx);
                    }
                    try {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                    } catch (Exception ignored) {
                    }
                }
                productAttributeRepository.save(pa);
            } else {
                List<ProductMedia> matches = productMediaRepository
                        .findByProductIdOrderByPositionAsc(req.productId())
                        .stream()
                        .filter(m -> m.getUrl().equals(req.url()))
                        .toList();
                if (matches.isEmpty()) {
                    return new ApiResponse<>(false, "Media not found", null, 404);
                }
                for (ProductMedia m : matches) {
                    try {
                        cloudinary.uploader().destroy(m.getPublicId(),
                                ObjectUtils.asMap("resource_type",
                                        m.getMediaType() == MediaType.VIDEO ? "video" : "image"));
                    } catch (Exception ignored) {
                    }
                    productMediaRepository.delete(m);
                }
            }

            return new ApiResponse<>(true, "Media removed", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Could not remove media: " + e.getMessage(), null, 500);
        }
    }

    /**
     * Whether this product may proceed past the Images step — requires a
     * cover photo/video and at least one image for every value of every
     * image-required attribute (e.g. one photo per selected Color).
     */
    @Transactional
    public ApiResponse<Object> getMediaStatus(UUID productId) {
        try {
            UUID sellerId = getUserId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (!product.getSeller().getId().equals(sellerId)) {
                return new ApiResponse<>(false, "Not authorized for this product", null, 403);
            }

            List<String> missing = new ArrayList<>();

            boolean hasCover = productMediaRepository.findByProductIdOrderByPositionAsc(productId)
                    .stream().anyMatch(ProductMedia::isCover);
            if (!hasCover) missing.add("Cover photo or video");

            for (ProductAttribute pa : product.getProductAttributes()) {
                boolean isImageAttr = Boolean.TRUE.equals(pa.getCategoryAttribute().getIsImageAttribute());
                if (isImageAttr && (pa.getImages() == null || pa.getImages().isEmpty())) {
                    missing.add(pa.getValue() + " photo/video");
                }
            }

            var dto = new com.ProductClientService.ProductClientService.DTO.seller.MediaStatusResponseDto(
                    missing.isEmpty(), missing);
            return new ApiResponse<>(true, missing.isEmpty() ? "Media complete" : "Missing required media", dto, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Could not check media status: " + e.getMessage(), null, 500);
        }
    }

    @Transactional
    public ApiResponse<Object> removeProductMedia(UUID mediaId) {
        try {
            UUID sellerId = getUserId();
            Optional<UUID> mediaSellerId = productMediaRepository.findSellerIdByMediaId(mediaId);
            if (mediaSellerId.isEmpty()) {
                return new ApiResponse<>(false, "Media not found", null, 404);
            }
            if (!mediaSellerId.get().equals(sellerId)) {
                return new ApiResponse<>(false, "Access denied", null, 403);
            }
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
            UUID sellerId = getUserId();
            Optional<UUID> mediaSellerId = productMediaRepository.findSellerIdByMediaId(mediaId);
            if (mediaSellerId.isEmpty()) {
                return new ApiResponse<>(false, "Media not found", null, 404);
            }
            if (!mediaSellerId.get().equals(sellerId)) {
                return new ApiResponse<>(false, "Access denied", null, 403);
            }
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
            UUID sellerId = getUserId();
            UUID ownerId = productRepository.findSellerIdByProductId(productId);
            if (ownerId == null) {
                return new ApiResponse<>(false, "Product not found", null, 404);
            }
            if (!ownerId.equals(sellerId)) {
                return new ApiResponse<>(false, "Access denied", null, 403);
            }

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

    // ── GET /api/v1/seller/product/{productId}/edit-data ─────────────────────
    @Transactional
    public ApiResponse<Object> getProductEditData(UUID productId) {
        UUID sellerId = getUserId();
        Product product = productRepository.findProductWithAttributesAndVariants(productId)
                .orElse(null);
        if (product == null)
            return new ApiResponse<>(false, "Product not found", null, 404);
        if (!product.getSeller().getId().equals(sellerId))
            return new ApiResponse<>(false, "Access denied", null, 403);

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", product.getId().toString());
        data.put("name", product.getName());
        data.put("description", product.getDescription());
        data.put("step", product.getStep().name());
        data.put("isActive", Boolean.TRUE.equals(product.getIsActive()));
        data.put("categoryId", product.getCategory() != null ? product.getCategory().getId().toString() : null);
        data.put("categoryName", product.getCategory() != null ? product.getCategory().getName() : null);
        data.put("brandId", product.getBrand() != null ? product.getBrand().getId().toString() : null);
        data.put("brandName", product.getBrand() != null ? product.getBrand().getName() : null);
        data.put("tags", product.getTags().stream()
                .map(t -> Map.of("id", t.getId().toString(), "name", t.getName()))
                .toList());
        data.put("attributes", product.getProductAttributes().stream()
                .filter(pa -> pa.getCategoryAttribute() != null)
                .map(pa -> {
                    String attrName = pa.getCategoryAttribute().getAttributes() != null
                            ? pa.getCategoryAttribute().getAttributes().stream()
                                    .findFirst().map(a -> a.getName()).orElse("")
                            : "";
                    return Map.of(
                            "productAttributeId", pa.getId().toString(),
                            "categoryAttributeId", pa.getCategoryAttribute().getId().toString(),
                            "name", attrName,
                            "value", pa.getValue() != null ? pa.getValue() : "",
                            "isVariant", Boolean.TRUE.equals(pa.getCategoryAttribute().getIsVariantAttribute()),
                            "isImage", Boolean.TRUE.equals(pa.getCategoryAttribute().getIsImageAttribute()),
                            "images", pa.getImages() != null ? pa.getImages() : List.of());
                }).toList());

        return new ApiResponse<>(true, "Product edit data", data, 200);
    }

    // ── GET /api/v1/seller/product/low-stock?threshold=5 ─────────────────────
    public ApiResponse<Object> getLowStockProducts(int threshold) {
        UUID sellerId = getUserId();
        int t = Math.max(0, Math.min(50, threshold));
        List<Object[]> rows = productRepository.findLowStockProductsBySeller(sellerId, t, 30);
        List<Map<String, Object>> result = rows.stream().map(row -> {
            Map<String, Object> p = new java.util.LinkedHashMap<>();
            p.put("id", row[0] != null ? row[0].toString() : null);
            p.put("name", row[1]);
            p.put("stock", row[2] != null ? ((Number) row[2]).intValue() : 0);
            long price = 0;
            try {
                price = Long.parseLong(row[3].toString());
            } catch (Exception ignored) {
            }
            p.put("priceRupees", String.format("%.2f", price / 100.0));
            p.put("imageUrl", row[4]);
            return p;
        }).toList();
        return new ApiResponse<>(true, "Low stock products", result, 200);
    }

    // ── GET /api/v1/seller/product/dashboard-summary ──────────────────────────
    public ApiResponse<Object> getDashboardSummary() {
        UUID sellerId = getUserId();
        int lowStockThreshold = 5;

        long totalProducts     = productRepository.countAllProductsBySeller(sellerId);
        long liveProducts      = productRepository.countLiveProductsBySeller(sellerId);
        long activeProducts    = productRepository.countActiveLiveProductsBySeller(sellerId);
        long outOfStockProducts = productRepository.countOutOfStockProductsBySeller(sellerId);
        long lowStockProducts  = productRepository.countLowStockProductsBySeller(sellerId, lowStockThreshold);

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("totalProducts",     totalProducts);
        data.put("liveProducts",      liveProducts);
        data.put("activeProducts",    activeProducts);
        data.put("outOfStockProducts", outOfStockProducts);
        data.put("lowStockProducts",  lowStockProducts);
        data.put("lowStockThreshold", lowStockThreshold);

        return new ApiResponse<>(true, "Dashboard summary fetched", data, 200);
    }

    // ── GET /api/v1/seller/product/{productId}/variants ──────────────────────
    public ApiResponse<Object> getProductVariants(UUID productId) {
        UUID sellerId = getUserId();
        UUID ownerId = productRepository.findSellerIdByProductId(productId);
        if (ownerId == null) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        if (!ownerId.equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        List<ProductVariant> variants = productVariantRepository.findByProductId(productId);
        List<Map<String, Object>> result = variants.stream().map(v -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", v.getId().toString());
            m.put("sku", v.getSku() != null ? v.getSku() : "");
            m.put("label", v.getLabel() != null ? v.getLabel() : "");
            long price = 0, mrp = 0;
            try {
                price = Long.parseLong(v.getPrice());
            } catch (Exception ignored) {
            }
            try {
                mrp = Long.parseLong(v.getMrp());
            } catch (Exception ignored) {
            }
            m.put("priceInPaise", price);
            m.put("priceRupees", String.format("%.2f", price / 100.0));
            m.put("mrpInPaise", mrp);
            m.put("mrpRupees", String.format("%.2f", mrp / 100.0));
            m.put("stock", v.getStock());
            m.put("combination", v.getCombination() != null ? v.getCombination() : Map.of());
            m.put("discount", toDiscountView(v));
            return m;
        }).toList();
        return new ApiResponse<>(true, "Variants fetched", result, 200);
    }

    // ── PATCH /api/v1/seller/product/{productId}/variants/{variantId} ─────────
    @Transactional
    public ApiResponse<Object> updateVariant(UUID productId, UUID variantId, Long priceInPaise, Integer stock) {
        UUID sellerId = getUserId();
        UUID ownerId = productRepository.findSellerIdByProductId(productId);
        if (ownerId == null) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        if (!ownerId.equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        if (!productVariantRepository.existsByIdAndProductIdAndSellerId(variantId, productId, sellerId)) {
            return new ApiResponse<>(false, "Variant not found", null, 404);
        }
        productVariantRepository.updateByIdAndProductId(variantId, productId, priceInPaise, stock);
        if (stock != null) {
            checkLowStockAndNotify(productId, sellerId);
        }
        return new ApiResponse<>(true, "Variant updated", java.util.Map.of("id", variantId.toString()), 200);
    }

    // ── PATCH /api/v1/seller/product/{productId}/variants/{variantId}/discount ─
    // Configures (creates or overwrites) a per-variant discount, independently of
    // price/stock. Recomputes the buyer-facing discount_price/discount_percentage
    // columns and reindexes the product to ES so search stays correct.
    @Transactional
    public ApiResponse<Object> configureVariantDiscount(UUID productId, UUID variantId,
            com.ProductClientService.ProductClientService.DTO.seller.VariantDiscountDto dto) {
        UUID sellerId = getUserId();
        UUID ownerId = productRepository.findSellerIdByProductId(productId);
        if (ownerId == null) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        if (!ownerId.equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        if (!productVariantRepository.existsByIdAndProductIdAndSellerId(variantId, productId, sellerId)) {
            return new ApiResponse<>(false, "Variant not found", null, 404);
        }
        if (dto.type() == com.ProductClientService.ProductClientService.Model.DiscountType.PERCENTAGE
                && dto.value() > 90) {
            return new ApiResponse<>(false, "Percentage discount cannot exceed 90%", null, 400);
        }
        if (dto.startsAt() != null && dto.endsAt() != null && !dto.startsAt().isBefore(dto.endsAt())) {
            return new ApiResponse<>(false, "startsAt must be before endsAt", null, 400);
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        variant.setDiscountType(dto.type());
        variant.setDiscountValue(String.valueOf(dto.value()));
        variant.setDiscountActive(dto.active());
        variant.setDiscountStartsAt(dto.startsAt());
        variant.setDiscountEndsAt(dto.endsAt());
        variant.recomputeEffectiveDiscount();
        variant = productVariantRepository.save(variant);

        reindexIfPossible(productId);

        return new ApiResponse<>(true, "Discount configured", toVariantResponseDto(variant), 200);
    }

    // ── DELETE /api/v1/seller/product/{productId}/variants/{variantId}/discount ─
    @Transactional
    public ApiResponse<Object> removeVariantDiscount(UUID productId, UUID variantId) {
        UUID sellerId = getUserId();
        UUID ownerId = productRepository.findSellerIdByProductId(productId);
        if (ownerId == null) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }
        if (!ownerId.equals(sellerId)) {
            return new ApiResponse<>(false, "Access denied", null, 403);
        }
        if (!productVariantRepository.existsByIdAndProductIdAndSellerId(variantId, productId, sellerId)) {
            return new ApiResponse<>(false, "Variant not found", null, 404);
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        variant.clearDiscount();
        variant = productVariantRepository.save(variant);

        reindexIfPossible(productId);

        return new ApiResponse<>(true, "Discount removed", toVariantResponseDto(variant), 200);
    }

    private void reindexIfPossible(UUID productId) {
        CompletableFuture.runAsync(() -> {
            try {
                elasticsearchProductIndexer.indexProduct(productId);
            } catch (Exception e) {
                log.warn("ES re-index failed for productId={}: {}", productId, e.getMessage());
            }
        });
    }

    private com.ProductClientService.ProductClientService.DTO.seller.ProductVariantResponseDto.DiscountView toDiscountView(
            ProductVariant v) {
        if (v.getDiscountType() == null) {
            return null;
        }
        double value = 0;
        try {
            value = Double.parseDouble(v.getDiscountValue());
        } catch (Exception ignored) {
        }
        boolean effective = v.isDiscountCurrentlyEffective();
        Double effectivePrice = null;
        Double effectivePercentage = null;
        if (effective) {
            try {
                effectivePrice = Double.parseDouble(v.getDiscountPrice());
            } catch (Exception ignored) {
            }
            try {
                effectivePercentage = Double.parseDouble(v.getDiscountPercentage());
            } catch (Exception ignored) {
            }
        }
        return new com.ProductClientService.ProductClientService.DTO.seller.ProductVariantResponseDto.DiscountView(
                v.getDiscountType(), value, Boolean.TRUE.equals(v.getDiscountActive()),
                v.getDiscountStartsAt(), v.getDiscountEndsAt(),
                effective, effectivePrice, effectivePercentage);
    }

    private com.ProductClientService.ProductClientService.DTO.seller.ProductVariantResponseDto toVariantResponseDto(
            ProductVariant v) {
        return new com.ProductClientService.ProductClientService.DTO.seller.ProductVariantResponseDto(
                v.getId(), v.getSku(), v.getPrice(), v.getStock(), toDiscountView(v));
    }

    /**
     * Notify the seller when a product's total variant stock drops to/below the
     * low-stock threshold (matches the /low-stock?threshold=5 default). Publishes
     * to OrderPaymentNotificationService's shared notification pipeline.
     */
    private static final int LOW_STOCK_THRESHOLD = 5;

    private void checkLowStockAndNotify(UUID productId, UUID sellerId) {
        try {
            List<Object[]> rows = productRepository.findStockAndActiveByIds(List.of(productId));
            if (rows.isEmpty()) return;
            Object[] row = rows.get(0);
            long totalStock = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            boolean isActive = row[2] != null && (Boolean) row[2];
            if (!isActive || totalStock > LOW_STOCK_THRESHOLD) return;

            String productName = productRepository.findNameByIdAndSellerId(productId, sellerId).orElse("your product");
            sellerNotificationPublisher.publish(
                    sellerId,
                    "PRODUCT_UPDATES",
                    totalStock == 0 ? "Product out of stock" : "Low stock alert",
                    totalStock == 0
                            ? "\"" + productName + "\" is out of stock."
                            : "\"" + productName + "\" is low on stock (" + totalStock + " left).",
                    "/products/" + productId,
                    productId.toString(),
                    java.util.Map.of("productId", productId.toString(), "stock", String.valueOf(totalStock)));
        } catch (Exception e) {
            log.warn("Low-stock notification check failed for productId={}: {}", productId, e.getMessage());
        }
    }

    // ── GET /reviews ────────────────────────────────────────────────────────────
    public ApiResponse<Object> getSellerReviews(int page, int size, String query) {
        UUID sellerId = getUserId();
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = page * safeSize;
        String safeQuery = (query == null || query.isBlank()) ? null : query.trim();
        List<Object[]> rows = productRatingRepository.findReviewsBySeller(sellerId, safeQuery, safeSize, offset);
        long total = productRatingRepository.countReviewsBySeller(sellerId, safeQuery);

        List<Map<String, Object>> reviews = rows.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r[0] != null ? r[0].toString() : "");
            m.put("rating", r[1] != null ? ((Number) r[1]).intValue() : 0);
            m.put("title", r[2] != null ? r[2].toString() : "");
            m.put("review", r[3] != null ? r[3].toString() : "");
            m.put("helpfulCount", r[4] != null ? ((Number) r[4]).intValue() : 0);
            m.put("verifiedPurchase", r[5] != null && (Boolean) r[5]);
            m.put("createdAt", r[6] != null ? r[6].toString() : "");
            m.put("productId", r[7] != null ? r[7].toString() : "");
            m.put("productName", r[8] != null ? r[8].toString() : "");
            m.put("categoryName", r[9] != null ? r[9].toString() : "");
            m.put("productImageUrl", r[10] != null ? r[10].toString() : "");
            m.put("reviewerId", r[11] != null ? r[11].toString() : "");
            m.put("reviewerName", r[12] != null ? r[12].toString() : "Anonymous");
            m.put("reviewerAvatarUrl", r[13] != null ? r[13].toString() : "");
            m.put("sellerReply", r[14] != null ? r[14].toString() : "");
            m.put("sellerReplyAt", r[15] != null ? r[15].toString() : "");
            m.put("sellerReaction", r[16] != null ? r[16].toString() : "");
            return m;
        }).toList();

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("reviews", reviews);
        payload.put("page", page);
        payload.put("size", safeSize);
        payload.put("totalElements", total);
        payload.put("totalPages", (int) Math.ceil((double) total / safeSize));
        payload.put("hasMore", (long) (page + 1) * safeSize < total);
        return new ApiResponse<>(true, "Reviews fetched", payload, 200);
    }

    // ── DELETE /reviews/{reviewId} — seller moderates a comment on their own product ──
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<Object> deleteSellerReview(UUID reviewId) {
        UUID sellerId = getUserId();

        if (!productRatingRepository.existsByIdAndSellerId(reviewId, sellerId)) {
            return new ApiResponse<>(false, "Comment not found", null, 404);
        }

        com.ProductClientService.ProductClientService.Model.ProductRating review =
                productRatingRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return new ApiResponse<>(false, "Comment not found", null, 404);
        }

        UUID productId = review.getProduct().getId();
        reviewLikeRepository.deleteByReviewId(reviewId);
        productRatingRepository.delete(review);
        reviewService.updateProductRatingSummaryAsync(productId);

        return new ApiResponse<>(true, "Comment deleted", null, 200);
    }

    // ── POST /reviews/{reviewId}/reply — seller replies to a comment ──────────
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<Object> replySellerReview(UUID reviewId, String replyText) {
        UUID sellerId = getUserId();

        if (replyText == null || replyText.isBlank()) {
            return new ApiResponse<>(false, "Reply cannot be empty", null, 400);
        }
        if (!productRatingRepository.existsByIdAndSellerId(reviewId, sellerId)) {
            return new ApiResponse<>(false, "Comment not found", null, 404);
        }

        com.ProductClientService.ProductClientService.Model.ProductRating review =
                productRatingRepository.findById(reviewId).orElseThrow();
        review.setSellerReply(replyText.replaceAll("<[^>]*>", "").trim());
        review.setSellerReplyAt(java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")));
        productRatingRepository.save(review);

        return new ApiResponse<>(true, "Reply posted", Map.of(
                "sellerReply", review.getSellerReply(),
                "sellerReplyAt", review.getSellerReplyAt().toString()), 200);
    }

    // ── POST /customers/notify — seller sends a personal message/push to buyers ─
    // Reuses the existing seller-notification Kafka pipeline (already consumed by
    // OrderPaymentNotificationService for DB-persisted + FCM delivery) instead of
    // building a separate chat/messaging system.
    public ApiResponse<Object> notifyCustomers(List<UUID> userIds, String message) {
        if (userIds == null || userIds.isEmpty()) {
            return new ApiResponse<>(false, "No recipients selected", null, 400);
        }
        if (message == null || message.isBlank()) {
            return new ApiResponse<>(false, "Message cannot be empty", null, 400);
        }
        String safeMessage = message.replaceAll("<[^>]*>", "").trim();
        List<UUID> recipients = userIds.stream().distinct().limit(100).toList();

        for (UUID userId : recipients) {
            sellerNotificationPublisher.publish(
                    userId,
                    "SELLER_MESSAGE",
                    "New message from a seller you follow",
                    safeMessage,
                    null,
                    userId.toString(),
                    Map.of("type", "SELLER_MESSAGE"));
        }

        return new ApiResponse<>(true, "Message sent to " + recipients.size() + " customer(s)",
                Map.of("sentCount", recipients.size()), 200);
    }

    // ── POST /reviews/{reviewId}/react — seller toggles a quick emoji reaction ─
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<Object> reactToSellerReview(UUID reviewId, String emoji) {
        UUID sellerId = getUserId();

        if (!productRatingRepository.existsByIdAndSellerId(reviewId, sellerId)) {
            return new ApiResponse<>(false, "Comment not found", null, 404);
        }

        com.ProductClientService.ProductClientService.Model.ProductRating review =
                productRatingRepository.findById(reviewId).orElseThrow();
        boolean cleared = emoji != null && emoji.equals(review.getSellerReaction());
        review.setSellerReaction(cleared ? null : emoji);
        productRatingRepository.save(review);

        return new ApiResponse<>(true, cleared ? "Reaction removed" : "Reaction saved",
                Map.of("sellerReaction", review.getSellerReaction() == null ? "" : review.getSellerReaction()), 200);
    }

    // ── GET /reviews/summary ─────────────────────────────────────────────────
    public ApiResponse<Object> getSellerReviewSummary() {
        UUID sellerId = getUserId();

        List<Object[]> avgRow = productRatingRepository.findSellerRatingSummary(sellerId);
        double avgRating = 0.0;
        long totalCount = 0L;
        if (!avgRow.isEmpty() && avgRow.get(0)[0] != null) {
            avgRating = ((Number) avgRow.get(0)[0]).doubleValue();
            totalCount = ((Number) avgRow.get(0)[1]).longValue();
        }

        List<Object[]> distRows = productRatingRepository.findSellerStarDistribution(sellerId);
        Map<String, Long> distribution = new java.util.LinkedHashMap<>();
        for (int i = 5; i >= 1; i--)
            distribution.put(String.valueOf(i), 0L);
        for (Object[] row : distRows) {
            String star = String.valueOf(((Number) row[0]).intValue());
            distribution.put(star, ((Number) row[1]).longValue());
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("avgRating", Math.round(avgRating * 10.0) / 10.0);
        payload.put("totalCount", totalCount);
        payload.put("distribution", distribution);
        return new ApiResponse<>(true, "Review summary", payload, 200);
    }
}
// hukiiu iuui jkjbhjhhjhj huhu uhh,j uh yiu ujhhuhjuhui uhh juyyuuik uhhu
// huiu8i iyu iy7uiu8u8uiui hujij juji uhj hij iji ijji
