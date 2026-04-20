package com.ProductClientService.ProductClientService.Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import com.ProductClientService.ProductClientService.Configuration.CacheConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.CategoryTreeDTO;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.ProductRatingDTO;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesDTO;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesProjection;
import com.ProductClientService.ProductClientService.DTO.SingleProductDetailDto;
import com.ProductClientService.ProductClientService.DTO.Cart.CategoryTreeProjection;
import com.ProductClientService.ProductClientService.DTO.search.SearchRequest;
import com.ProductClientService.ProductClientService.DTO.search.SearchResultsResponse;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.AttributeRepositoryImpl;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.SectionRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final SectionRepository sectionRepository;
    private final ProductRatingRepository productRatingRepository;
    private final UserRepojectory userRepojectory;
    private final ObjectMapper objectMapper;
    private final AttributeRepositoryImpl attributeRepositoryImpl;
    private final HttpServletRequest request;
    private final EventPublisherService eventPublisher;
    private final SearchResultsService searchResultsService;

    public ApiResponse<Object> searchProducts(SearchRequest req, boolean includeFilter, UUID userId) {
        SearchResultsResponse products = searchResultsService.search(req, userId);

        if (!includeFilter) {
            return new ApiResponse<>(true, "Fetched products", products, 200);
        }

        // Include attribute filters for the category alongside product cards
        List<AttributeDto> filters = req.getCategoryId() != null
                ? attributeRepositoryImpl.findFiltersByCategoryId(req.getCategoryId())
                : List.of();

        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("filters", filters);
        return new ApiResponse<>(true, "Fetched products and filters", response, 200);
    }

    public ApiResponse<Object> searchProducts(String keyword) {

        List<ProductWithImagesProjection> products = productRepository.searchProductsWithImages(keyword);

        List<ProductWithImagesDTO> productList = products.stream()
                .map(p -> new ProductWithImagesDTO(
                        p.getId(),
                        p.getName(),
                        p.getDescription(),
                        p.getImages()))
                .toList();

        // Fetch brands
        List<Brand> brands = brandRepository.searchBrands(keyword);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("products", productList);
        response.put("brands", brands);
        return new ApiResponse<>(true, "Fetched products and brands", response, 200);
    }

    @Cacheable(value = CacheConfig.CATEGORY_TREE, key = "'all'")
    public ApiResponse<Object> getFullCategoryTree() {

        try {

            List<CategoryTreeProjection> allCategories = categoryRepository.fetchCategoryTreeData();

            Map<UUID, CategoryTreeDTO> categoryMap = new HashMap<>();
            List<CategoryTreeDTO> rootCategories = new ArrayList<>();

            // Convert projection → DTO and put in map
            for (CategoryTreeProjection p : allCategories) {

                CategoryTreeDTO dto = new CategoryTreeDTO(
                        p.getId(),
                        p.getName(),
                        p.getImageUrl(),
                        p.getCategoryLevel(),
                        new ArrayList<>());

                categoryMap.put(dto.getId(), dto);
            }

            // Build tree
            for (CategoryTreeProjection p : allCategories) {

                CategoryTreeDTO current = categoryMap.get(p.getId());

                if (p.getParent_Id() != null) {

                    CategoryTreeDTO parent = categoryMap.get(p.getParent_Id());

                    if (parent != null) {
                        parent.getChildren().add(current);
                    }

                } else {
                    rootCategories.add(current);
                }
            }

            return new ApiResponse<>(
                    true,
                    "Full category tree fetched successfully",
                    rootCategories,
                    200);

        } catch (Exception e) {

            return new ApiResponse<>(
                    false,
                    e.getMessage(),
                    null,
                    500);
        }
    }

    public ApiResponse<Object> getCategoryLevelWise(Category.Level level) {
        try {
            List<CategoryProjection> categories = categoryRepository.findByCategoryLevel(level);
            return new ApiResponse<>(true, "Categories fetched successfully", categories, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public List<Category> getCategoriesByParentIds(List<UUID> parentIds) {
        return categoryRepository.findByParentIdIn(parentIds);
    }

    public ApiResponse<Object> getSectionsByCategory(String category) {
        try {
            List<Section> response = sectionRepository.findActiveSectionsByCategory(category);
            return new ApiResponse<>(true, "Get Sections", response, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }

    }

    public ApiResponse<Object> getProductDetail(UUID productId) {
        try {
            String response = productRepository.findSnapshotById(productId)
                    .filter(s -> !s.isBlank())
                    .orElseGet(() -> productRepository.getProductDetailAsJson(productId));
            if (response != null) {
                // Publish view event asynchronously — never blocks the response
                UUID userId = tryGetUserId();
                UUID categoryId = productRepository.findCategoryIdByProductId(productId).orElse(null);
                eventPublisher.publishProductViewed(productId, userId, categoryId);
                return new ApiResponse<>(true, "Get Product Detail", objectMapper.readTree(response), 200);
            } else {
                return new ApiResponse<>(false, "Product not found", null, 404);
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    /** Returns userId from the JWT if authenticated, null for guest requests. */
    private UUID tryGetUserId() {
        try {
            return getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    // add rating
    public ApiResponse<Object> createOrUpdateRating(UUID productId, int rating, String review) {
        System.out.println("in service function");

        System.out.println("in service function now productId" + getUserId());
        boolean exists = productRepository.existsById(productId);
        if (!exists) {
            throw new RuntimeException("Product not found");
        }

        Product productRef = new Product();
        productRef.setId(productId);
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("user found" + user);
        // Check if rating already exists
        Optional<ProductRating> existingRating = productRatingRepository
                .findByProductIdAndUserId(productId, getUserId());
        System.out.println("Rating found" + existingRating);
        if (existingRating.isPresent()) {
            ProductRating pr = existingRating.get();
            pr.setRating(rating);
            pr.setReview(review);
            productRatingRepository.save(pr);
            System.out.println("Updated");
            updateProductRatingSummary(productId);
            return new ApiResponse(true, "Review Updated", null, 201);
        } else {
            ProductRating pr = new ProductRating();
            pr.setProduct(productRef);
            pr.setUser(user);
            pr.setRating(rating);
            pr.setReview(review);
            productRatingRepository.save(pr); // new insert
            System.out.println("Saved");
            updateProductRatingSummary(productId);
            return new ApiResponse(true, "Review Added", null, 201);
        }
    }

    @Transactional
    @Async
    private void updateProductRatingSummary(UUID productId) {
        List<Object[]> results = productRatingRepository.findAvgAndCountByProductId(productId);
        Double avgRating = 0.0;
        Integer ratingCount = 0;

        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            avgRating = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
            ratingCount = row[1] != null ? ((Number) row[1]).intValue() : 0;
        }

        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.updateProductRating(productId, avgRating, ratingCount);
    }

    // Get ratings list
    public ApiResponse<Object> getRatingsByProduct(UUID productId) {
        try {
            List<ProductRating> ratings = productRatingRepository.findByProductId(productId);

            List<ProductRatingDTO> dtos = ratings.stream()
                    .map(ProductRatingDTO::fromEntity)
                    .toList();
            return new ApiResponse<>(true, "Get Ratings", dtos, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    // Get rating summary
    public ApiResponse<Object> getRatingSummary(UUID productId) {
        try {
            Double avgRating = productRatingRepository.findAverageRatingByProductId(productId);
            Long totalRatings = productRatingRepository.countRatingsByProductId(productId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("averageRating", avgRating != null ? avgRating : 0.0);
            summary.put("totalRatings", totalRatings);

            return new ApiResponse<>(true, "Get Rating Summary", summary, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}

/// bkhhkuhjhjkfhiuh hiujfik mbhuyg jhguky gfyugjyghvtfujyg hgvytfgmm hguygug
// jjuj huhu huhu jujn njjn