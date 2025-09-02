package com.ProductClientService.ProductClientService.Service.seller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDocument;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.CategoryAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductFullResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantResponseDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.listener.event.ProductUpdatedEvent;
import com.ProductClientService.ProductClientService.Repository.AttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.ProductAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Service.S3Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
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
    @PersistenceContext
    private EntityManager entityManager;

    public SellerService(ProductRepository productRepository, S3Service s3Service,
            CategoryRepository categoryRepository, HttpServletRequest request,
            CategoryAttributeRepository categoryAttributeRepository,
            AttributeRepository attributeRepository,
            ProductAttributeRepository productAttributeRepository,
            ProductVariantRepository productVariantRepository, ElasticsearchClient elasticsearchClient,
            StandardProductRepository standardProductRepository) {
        this.productRepository = productRepository;
        this.s3Service = s3Service;
        this.categoryRepository = categoryRepository;
        this.request = request;
        this.categoryAttributeRepository = categoryAttributeRepository;
        this.attributeRepository = attributeRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.productVariantRepository = productVariantRepository;
        this.elasticsearchClient = elasticsearchClient;
        this.standardProductRepository = standardProductRepository;
    }

    public ApiResponse<Object> addProduct(ProductDto dto) throws IOException {
        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setStep(Product.Step.valueOf(dto.step()));
        Seller sellerRef = entityManager.getReference(Seller.class, (UUID) request.getAttribute("id"));
        product.setSeller(sellerRef);
        if (dto.category() != null) {
            Category categoryRef = entityManager.getReference(Category.class, (UUID) dto.category());
            product.setCategory(categoryRef);
        }
        List<String> imageUrls = new ArrayList<>();
        // if (dto.images() != null && !dto.images().isEmpty()) {
        // imageUrls = s3Service.uploadFiles(dto.images());
        // }

        // if (!imageUrls.isEmpty())
        // product.setProductImage(imageUrls);

        boolean isSaved = productRepository.save(product) != null;
        if (!isSaved)
            return new ApiResponse<>(false, "Step Not Completed", null, 200);
        return new ApiResponse<>(true, "Step Completed", null, 200);
    }

    public ApiResponse<Object> loadAttribute(UUID id) {
        return new ApiResponse<>(true, "Step Completed", null, 200);
    }

    public ApiResponse<Object> getAttributesByCategoryId(UUID categoryId) {
        System.out.println("we are calling " + categoryId);

        Optional<CategoryAttribute> categoryAttributes = categoryAttributeRepository.findByCategoryId(categoryId);

        if (categoryAttributes.isPresent()) {
            CategoryAttribute categoryAttribute = categoryAttributes.get();

            CategoryAttributeDto dto = new CategoryAttributeDto(
                    categoryAttribute.getId(),
                    categoryAttribute.getCategory().getId(),
                    categoryAttribute.getAttributes()
                            .stream()
                            .map(attr -> new AttributeDto(
                                    attr.getName(),
                                    attr.getField_type(), // assuming FEILDTYPE is an enum in Attribute
                                    attr.getIs_required(),
                                    attr.getOptions()))
                            .toList());

            return new ApiResponse<>(true, "fetch data", dto, 200);
        } else {
            return new ApiResponse<>(false, "No attributes found for this category", null, 404);
        }
    }

    public ApiResponse<Object> addProductAttribute(ProductAttributeDto dto) {
        try {
            saveAllAttributes(dto);
            return new ApiResponse<>(true, "Saved In The Db", null, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 501);
        }
    }

    @Transactional
    public void saveAllAttributes(ProductAttributeDto dto) {
        // ✅ Fetch existing product
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStep(Product.Step.valueOf(dto.step()));
        // ✅ Build product attributes
        for (int i = 0; i < dto.attributeId().size(); i++) {
            UUID attrId = dto.attributeId().get(i);
            List<String> vals = dto.values().get(i);

            for (String val : vals) {
                ProductAttribute pa = new ProductAttribute();

                Attribute attribute = new Attribute();
                attribute.setId(attrId);
                pa.setAttribute(attribute);
                pa.setProduct(product);
                pa.setValue(val);

                product.getProductAttributes().add(pa);
            }
        }

        productRepository.save(product);
    }

    public ApiResponse<Object> getProductAttributes(UUID productId) {
        try {
            List<ProductAttribute> attributes = productAttributeRepository.findByProductIdWithAttribute(productId);

            // Map to DTO for clean response
            List<Map<String, Object>> response = attributes.stream().map(attr -> {
                Map<String, Object> map = new HashMap<>();
                map.put("attributeId", attr.getAttribute().getId());
                map.put("attributeName", attr.getAttribute().getName()); // if you want name too
                map.put("value", attr.getValue());
                return map;
            }).toList();

            return new ApiResponse<>(true, "Fetched successfully", response, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> addProductVariants(ProductVariantsDto dto) {
        try {
            List<ProductVariant> variants = new ArrayList<>();
            List<ProductAttribute> attrcollection = new ArrayList<>();
            for (int i = 0; i < dto.productAttributeId().size(); i++) {
                UUID productattributeId = dto.productAttributeId().get(i);

                ProductAttribute attributeOpt = productAttributeRepository.findById(productattributeId)
                        .orElseThrow(() -> new RuntimeException("Invalid attributeId:" + productattributeId));
                ProductVariant variant = new ProductVariant();
                variant.setSku(dto.skus().get(i));
                variant.setPrice(dto.price().get(i));
                variant.setStock(Integer.parseInt(dto.stock().get(i)));
                variants.add(variant);
                attributeOpt.getVariants().add(variant);
            }

            productVariantRepository.saveAll(variants);
            Product product = productRepository.findByProductAttributeId(dto.productAttributeId().get(0))
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            product.setStep(Product.Step.valueOf(dto.step()));
            return new ApiResponse<>(true, "Variants added successfully", variants, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> getProductWithAttributesAndVariants(UUID productId) {
        try {
            Product product = productRepository.findProductWithAttributesAndVariants(productId)
                    .orElseThrow(() -> new RuntimeException("Invalid productId: " + productId));
            List<ProductAttributeResponseDto> attributesDto = product.getProductAttributes().stream()
                    .map(attr -> new ProductAttributeResponseDto(
                            attr.getId(),
                            attr.getAttribute().getName(),
                            attr.getValue(), // adjust if you store differently
                            attr.getVariants().stream()
                                    .map(variant -> new ProductVariantResponseDto(
                                            variant.getId(),
                                            variant.getSku(),
                                            variant.getPrice(),
                                            variant.getStock()))
                                    .toList()))
                    .toList();

            ProductFullResponseDto responseDto = new ProductFullResponseDto(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    attributesDto);

            return new ApiResponse<>(true, "Product details fetched successfully", responseDto, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 500);
        }
    }

    public ApiResponse<Object> MakeProductLive(UUID productId) {
        try {
            Product.Step currentStep = productRepository.findStepById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (currentStep == Product.Step.PRODUCT_VARIANT) {
                int updatescore = updateStatusById(productId, Product.Step.LIVE);
                if (updatescore > 0) {
                    indexProduct(productId);
                    handleProductUpdate(productId);
                    return new ApiResponse<>(true, "Product Live", null, 200);
                } else {
                    return new ApiResponse<>(false, "Interna; Server Error", null, 500);
                }

            } else
                return new ApiResponse<>(false, "Bad Request", null, 403);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Something went wrong: " + e.getMessage(), null, 500);
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
            try {
                System.out.println("product value" + product.getName() + " " + product.getDescription() + " " +
                        product.getProductImages() + " " + product.getCategory() + " " + product.getBrand());
                StandardProduct standardProduct = new StandardProduct();
                standardProduct.setName(product.getName());
                standardProduct.setDescription(product.getDescription());
                standardProduct.setProductImages(new HashSet<>(product.getProductImages()));
                standardProduct.setCategory(product.getCategory());
                standardProduct.setBrandEntity(product.getBrand());

                StandardProduct saved = standardProductRepository.save(standardProduct);
                System.out.println("✅ StandardProduct saved: " + saved);

            } catch (Exception e) {
                System.err.println("❌ Failed to save StandardProduct: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Product not standard/live, skipping StandardProduct creation.");
        }
    }

    @Async
    public void indexProduct(UUID productId) throws IOException {
        try {
            ProductElasticDto dto = productRepository.findProductForIndexing(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            ProductDocument productDoc = ProductDocument.builder()
                    .id(dto.getId().toString())
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .sellerId(dto.getSellerId().toString())
                    .sellerName(dto.getSellerName())
                    .categoryId(dto.getCategoryId().toString())
                    .categoryName(dto.getCategoryName())
                    .brandId(dto.getBrandId() != null ? dto.getBrandId().toString() : null)
                    .brandName(dto.getBrandName())
                    .createdAt(dto.getCreatedAt().toString())
                    .build();

            elasticsearchClient.index(i -> i
                    .index("products")
                    .id(productDoc.getId())
                    .document(productDoc));
        } catch (Exception e) {
            System.err.println("❌ Failed to indexProduct " + e.getMessage());
            e.printStackTrace();
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
}

// hihiy ugytubhjguy gyutubhgu kjhk  jhbuy jbiuh hjughu