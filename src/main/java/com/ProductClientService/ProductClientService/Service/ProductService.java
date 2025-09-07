package com.ProductClientService.ProductClientService.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.SingleProductDetailDto;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.Section;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository;
import com.ProductClientService.ProductClientService.Repository.SectionRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ProductClientService.ProductClientService.Model.User;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SectionRepository sectionRepository;
    private final ProductRatingRepository productRatingRepository;
    private final UserRepojectory userRepojectory;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)

    public List<Product> searchProducts(UUID categoryId, UUID brandId, UUID sellerId,
            String attributeName, String attributeValue) {
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

        return builder.execute(productSearchRepository);
    }

    public List<ProductElasticDto> searchProducts(String keyword) throws IOException {
        // 🔍 Build search query
        SearchResponse<ProductElasticDto> response = elasticsearchClient.search(s -> s
                .index("products") // your ES index name
                .query(q -> q
                        .multiMatch(m -> m
                                .fields("name", "description", "brandName", "categoryName", "sellerName") // fields to
                                                                                                          // search
                                .query(keyword)))
                .size(20) // limit results
                .from(0), // pagination start
                ProductElasticDto.class);

        List<ProductElasticDto> results = new ArrayList<>();
        for (Hit<ProductElasticDto> hit : response.hits().hits()) {
            results.add(hit.source());
        }

        return results;
    }

    public ApiResponse<Object> getCategory() {
        try {
            List<CategoryProjection> categories = categoryRepository.findByCategoryLevel(Category.Level.SUPER_CATEGORY);
            return new ApiResponse<>(true, "Get List", categories, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
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
    public ApiResponse<Object> createOrUpdateRating(UUID productId, UUID userId, int rating, String review) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = userRepojectory.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if rating already exists
        Optional<ProductRating> existingRating = productRatingRepository
                .findByProductIdAndUserId(productId, userId);

        if (existingRating.isPresent()) {
            ProductRating pr = existingRating.get();
            pr.setRating(rating);
            pr.setReview(review);
            productRatingRepository.save(pr);
            updateProductRatingSummary(productId);
            return new ApiResponse(true, "Review Updated", null, 201);
        } else {
            ProductRating pr = new ProductRating();
            pr.setProduct(product);
            pr.setUser(user);
            pr.setRating(rating);
            pr.setReview(review);
            productRatingRepository.save(pr); // new insert
            updateProductRatingSummary(productId);
            return new ApiResponse(true, "Review Added", null, 201);
        }
    }

    @Async
    private void updateProductRatingSummary(UUID productId) {
        Object[] avgAndCount = productRatingRepository.findAvgAndCountByProductId(productId);
        Double avgRating = avgAndCount[0] != null ? (Double) avgAndCount[0] : 0.0;
        Long ratingCount = avgAndCount[1] != null ? (Long) avgAndCount[1] : 0L;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setAverageRating(avgRating);
        product.setRatingCount(ratingCount.intValue());
        productRepository.save(product);
    }

    // Get ratings list
    public ApiResponse<Object> getRatingsByProduct(UUID productId) {
        try {
            List<ProductRating> ratings = productRatingRepository.findByProductId(productId);
            return new ApiResponse<>(true, "Get Ratings", ratings, 200);
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
/// jggug kjnhnhu jbuyhhihu khukhyuhy nmguyy uhh mknkjnknhj nb jhbjnm khkhkuhhku
