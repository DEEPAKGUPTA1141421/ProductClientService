package com.ProductClientService.ProductClientService.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchClient elasticsearchClient;

    private final CategoryRepository categoryRepository;
    // private final ElasticsearchFeignClient esClient;

    public ProductService(ProductSearchRepository productSearchRepository,
            ElasticsearchClient elasticsearchClient, CategoryRepository categoryRepository) {
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchClient = elasticsearchClient;
        this.categoryRepository = categoryRepository;
        // this.esClient = esClient;
    }

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
}
/// bkhhkuhjhjkfhiuh hiujfik mbhuyg jhguky gfyugjyghvtfujyg hgvytfgmm