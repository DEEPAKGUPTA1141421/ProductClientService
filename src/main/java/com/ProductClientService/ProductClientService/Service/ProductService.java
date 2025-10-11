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

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.ProductRatingDTO;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesDTO;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesProjection;
import com.ProductClientService.ProductClientService.DTO.SingleProductDetailDto;
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
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository.ProductSearchDto;
import com.ProductClientService.ProductClientService.Repository.SectionRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ProductClientService.ProductClientService.Model.User;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final SectionRepository sectionRepository;
    private final ProductRatingRepository productRatingRepository;
    private final UserRepojectory userRepojectory;
    private final ObjectMapper objectMapper;
    private final AttributeRepositoryImpl attributeRepositoryImpl;
    private final HttpServletRequest request;

    @Transactional(readOnly = true)

    public ApiResponse<Object> searchProducts(UUID categoryId, UUID brandId, UUID sellerId,
            String attributeName, String attributeValue, boolean includeFilter) {
        ProductSearchBuilder builder = new ProductSearchBuilder();

        if (categoryId != null)
            builder.category(categoryId);
        if (brandId != null)
            builder.brand(brandId);
        if (sellerId != null)
            builder.seller(sellerId);
        if (attributeName != null && attributeValue != null) {
            builder.attribute(attributeName, attributeValue);
        }

        List<ProductSearchDto> products = builder.execute(productSearchRepository);
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        if (includeFilter) {
            System.out.println("Category ID for filters: " + categoryId);
            List<AttributeDto> filters = attributeRepositoryImpl.findFiltersByCategoryId(categoryId);
            response.put("filters", filters);
            System.out.println("Filters: " + filters);
        }
        return new ApiResponse<>(true, "Fetched products and brands", response, 200);

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

    public ApiResponse<Object> getCategory(boolean includeChildItem, Category.Level level) {
        try {
            short lvl = (short) level.ordinal();
            System.out.println("Condition check: " + includeChildItem);

            Object categories;

            if (includeChildItem) {
                // Fetch top 10 parent categories with children
                List<Map<String, Object>> parentCategoriesRaw = categoryRepository.findTop10ParentWithChildren(lvl);
                List<Map<String, Object>> parentCategories = new ArrayList<>();

                ObjectMapper objectMapper = new ObjectMapper();

                for (Map<String, Object> parent : parentCategoriesRaw) {
                    // Copy to a modifiable map
                    Map<String, Object> modifiableParent = new HashMap<>(parent);

                    Object children = modifiableParent.get("children");
                    if (children instanceof String) {
                        modifiableParent.put("children", objectMapper.readValue((String) children, List.class));
                    }

                    parentCategories.add(modifiableParent);
                }
                categories = parentCategories; // assign to outer variable
            } else {
                // Fetch top 10 categories without children
                categories = categoryRepository.findTop10ByLevel(lvl);
            }
            return new ApiResponse<>(true, "Fetched categories successfully", categories, 200);

        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(false, "Error: " + e.getMessage(), null, 500);
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
            String response = productRepository.getProductDetailAsJson(productId);
            if (response != null) {
                return new ApiResponse<>(true, "Get Product Detail", objectMapper.readTree(response), 200);
            } else {
                return new ApiResponse<>(false, "Product not found", null, 404);
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    // add rating
    public ApiResponse<Object> createOrUpdateRating(UUID productId, int rating, String review) {
        System.out.println("in service function");

        UUID userId = (UUID) request.getAttribute("id");
        System.out.println("in service function now productId" + userId);
        boolean exists = productRepository.existsById(productId);
        if (!exists) {
            throw new RuntimeException("Product not found");
        }

        Product productRef = new Product();
        productRef.setId(productId);
        User user = userRepojectory.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("user found" + user);
        // Check if rating already exists
        Optional<ProductRating> existingRating = productRatingRepository
                .findByProductIdAndUserId(productId, userId);
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
}

/// bkhhkuhjhjkfhiuh hiujfik mbhuyg jhguky gfyugjyghvtfujyg hgvytfgmm hguygug
// y yiyi huiuyi yuyuhuhu huiuuhuuuyyyyythbvcertyuiolkjnhgfdrtyuio hkuhu iuu
// gyir yiyuir u88r yuiuiruu jiiopo jiiki jioii jioi jioio hiuuoiii
/// nhhbhjmhbjhbjhjbhjhbjhbhbhbbhb jkhu,uiyuyu78 yuuy7y ttyuyuu yy