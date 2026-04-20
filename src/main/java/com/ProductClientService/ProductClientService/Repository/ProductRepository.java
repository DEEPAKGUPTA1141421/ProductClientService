package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.ProductAttributeSuggestionProjection;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.ProductPriceSuggestionProjection;
import com.ProductClientService.ProductClientService.DTO.ProductSuggestionProjection;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesProjection;
import com.ProductClientService.ProductClientService.DTO.SingleProductDetailDto;
import com.ProductClientService.ProductClientService.DTO.admin.ProductAttributeForIntentProjection;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.Product.Step;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSellerProjection;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSummaryProjection;

import jakarta.transaction.Transactional;

import java.util.Collection;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Batch-fetches seller IDs for a set of product IDs in a single query.
     * Used by CartService to group cart items by shop without N individual lookups.
     */
    @Query("SELECT p.id AS productId, p.seller.id AS sellerId FROM Product p WHERE p.id IN :productIds")
    List<ProductSellerProjection> findSellerIdsByProductIds(@Param("productIds") Collection<UUID> productIds);
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.step = :step WHERE p.id = :productId")
    int updateStatusById(@Param("productId") UUID productId, @Param("step") Step step);

    @Query("SELECT p.attributesSnapshot FROM Product p WHERE p.id = :productId")
    Optional<String> findSnapshotById(@Param("productId") UUID productId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.attributesSnapshot = :snapshot WHERE p.id = :productId")
    void updateSnapshot(@Param("productId") UUID productId, @Param("snapshot") String snapshot);

    boolean existsById(UUID id);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.productAttributes pa " +
            "LEFT JOIN FETCH p.variants " +
            "WHERE p.id = :productId")
    Optional<Product> findProductWithAttributesAndVariants(@Param("productId") UUID productId);

    @Query("SELECT p FROM Product p JOIN p.productAttributes pa WHERE pa.id = :productAttributeId")
    Optional<Product> findByProductAttributeId(@Param("productAttributeId") UUID productAttributeId);

    @Query("SELECT p.step FROM Product p WHERE p.id = :id")
    Optional<Product.Step> findStepById(@Param("id") UUID id);

    @Query("SELECT new com.ProductClientService.ProductClientService.DTO.ProductElasticDto(" +
            "p.id, p.name, p.description, s.id, s.legalName, c.id, c.name, b.id, b.name, p.createdAt) " +
            "FROM Product p " +
            "JOIN p.seller s " +
            "JOIN p.category c " +
            "LEFT JOIN p.brand b " +
            "WHERE p.id = :productId")
    Optional<ProductElasticDto> findProductForIndexing(@Param("productId") UUID productId);

    @Query(value = """
                                                          SELECT jsonb_build_object(
                                                              'id', p.id,
                                                              'name', p.name,
                                                              'description', p.description,
                                                              'step', p.step,
                                                              'is_standard', p.is_standard,
                                                              'created_at', p.created_at,
                                                              'updated_at', p.updated_at,
                                                              'seller', jsonb_build_object(
                                                                  'id', s.id,
                                                                  'name', s.display_name,
                                                                  'email', s.email
                                                              ),
                                                              'variants', COALESCE(
                                                                  jsonb_agg(DISTINCT jsonb_build_object(
                                                                      'id', pv.id,
                                                                      'price', pv.price,
                                                                      'sku', pv.sku,
                                                                      'stock', pv.stock
                                                                  )) FILTER (WHERE pv.id IS NOT NULL), '[]'::jsonb
                                                              ),
                                                              'product_attributes', (
                                      SELECT jsonb_agg(attr_group)
                                      FROM (
                                          SELECT jsonb_build_object(
                                              'category_attribute_id', ca.id,
                                              'is_required', ca.is_required,
                                              'is_image_attribute', ca.is_image_attribute,
                                              'is_variant_attribute', ca.is_variant_attribute,
            'images', COALESCE(array_agg(pa.images) FILTER (WHERE pa.images IS NOT NULL), '{}'),
                                              'values', array_agg(pa.value)
                                          ) AS attr_group
                                          FROM product_attributes pa
                                          JOIN category_attributes ca ON ca.id = pa.category_attribute_id
                                          WHERE pa.product_id = p.id
                                          GROUP BY ca.id, ca.is_required, ca.is_image_attribute, ca.is_variant_attribute
                                      ) grouped
                                  )
                                  ) AS product_detail
                                  FROM products p
                                  LEFT JOIN sellers s ON s.id = p.seller_id
                                  LEFT JOIN product_variants pv ON pv.product_id = p.id
                                  WHERE p.id = :productId
                                  GROUP BY p.id, s.id
                                  """, nativeQuery = true)
    String getProductDetailAsJson(@Param("productId") UUID productId);

    @Query("select p.seller.id from Product p where p.id = :productId")
    UUID findSellerIdByProductId(@Param("productId") UUID productId);

    @Query("SELECT p.category.id FROM Product p WHERE p.id = :productId")
    Optional<UUID> findCategoryIdByProductId(@Param("productId") UUID productId);

    @Query(value = """
            SELECT
                p.id AS id,
                p.name AS name,
                p.description AS description,
                COALESCE(
                    jsonb_agg(DISTINCT pa.images) FILTER (WHERE pa.images IS NOT NULL AND ca.is_image_attribute = true),
                    '[]'::jsonb
                ) AS images
            FROM products p
            LEFT JOIN product_attributes pa ON pa.product_id = p.id
            LEFT JOIN category_attributes ca ON ca.id = pa.category_attribute_id
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            GROUP BY p.id
            LIMIT 20
            """, nativeQuery = true)
    List<ProductWithImagesProjection> searchProductsWithImages(@Param("keyword") String keyword);

    @Query(value = """
            SELECT a.*
            FROM categories c
            JOIN category_attributes ca
                ON ca.category_id = c.id
            JOIN category_attribute_mapping cam
                ON cam.category_attribute_id = ca.id
            JOIN attributes a
                ON a.id = cam.attribute_id
            WHERE c.id = :categoryId
            """, nativeQuery = true)
    List<AttributeDto> findFiltersByCategoryId(@Param("categoryId") UUID categoryId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.averageRating = :avg, p.ratingCount = :count WHERE p.id = :id")
    int updateProductRating(@Param("id") UUID productId,
            @Param("avg") Double avgRating,
            @Param("count") Integer ratingCount);

    @Query("""
                SELECT p.name AS name, p.description AS description
                FROM Product p
                WHERE p.id = :id
            """)
    ProductSummaryProjection getProductNameAndDescription(@Param("id") UUID productId);

    @Query("""
                SELECT
                    p.id as id,
                    c.name as categoryName,
                    b.name as brandName,
                    b.id as brandId,
                    p.searchIntentCreated as searchIntentCreated
                FROM Product p
                LEFT JOIN p.category c
                LEFT JOIN p.brand b
                WHERE p.searchIntentCreated = false
            """)
    List<ProductSuggestionProjection> findProductsForSuggestion();

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.searchIntentCreated = true WHERE p.id = :id")
    void markSearchIntentCreated(@Param("id") UUID id);

    @Query("""
                SELECT
                    p.id as productId,
                    c.name as categoryName,
                    a.name as attributeName,
                    pa.value as attributeValue
                FROM Product p
                JOIN p.category c
                JOIN p.productAttributes pa
                JOIN pa.categoryAttribute ca
                JOIN ca.attributes a
                WHERE p.searchIntentCreated = false
            """)
    List<ProductAttributeSuggestionProjection> findAttributeSuggestions();

    @Query("""
                SELECT
                    p.id as productId,
                    c.name as categoryName,
                    MIN(v.price) as minPrice
                FROM Product p
                JOIN p.category c
                JOIN p.variants v
                WHERE p.searchIntentCreated = false
                GROUP BY p.id, c.name
            """)
    List<ProductPriceSuggestionProjection> findPriceSuggestions();

    @Modifying
    @Query("UPDATE Product p SET p.searchIntentCreated = true WHERE p.searchIntentCreated = false")
    void markAllSearchIntentCreated();

    @EntityGraph(attributePaths = {
            "productAttributes",
            "productAttributes.categoryAttribute",
            "productAttributes.categoryAttribute.attributes",
            "variants",
            "tags",
            "brand",
            "category"
    })
    Optional<Product> findTopBySellerIdAndStepNotOrderByCreatedAtDesc(
            UUID sellerId,
            Product.Step step);

    @Query(value = """
                    SELECT
                        p.id AS productId,
                        c.id AS categoryId,
                        c.name AS categoryName,
                        b.id AS brandId,
                        b.name AS brandName,
                        ca.name AS attributeName,   -- ✅ FIXED
                        pa.value AS attributeValue,
                        ca.is_variant_attribute AS isVariantAttribute,
                        ca.is_image_attribute AS isImageAttribute
                    FROM products p
                    JOIN categories c ON c.id = p.category_id
                    LEFT JOIN brands b ON b.id = p.brand_id
                    JOIN product_attributes pa ON pa.product_id = p.id
                    JOIN category_attributes ca ON ca.id = pa.category_attribute_id
                    WHERE p.id = :productId
            """, nativeQuery = true)
    List<ProductAttributeForIntentProjection> findAttributesForIntentByProductId(
            @Param("productId") UUID productId);
}

// hyuhk khui huih iui huiuhukuijkji