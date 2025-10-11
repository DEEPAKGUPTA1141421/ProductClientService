package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.ProductWithImagesProjection;
import com.ProductClientService.ProductClientService.DTO.SingleProductDetailDto;
import com.ProductClientService.ProductClientService.Model.Attribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.Product.Step;
import com.ProductClientService.ProductClientService.Repository.Projection.ProductSummaryProjection;

import jakarta.transaction.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.step = :step WHERE p.id = :productId")
    int updateStatusById(@Param("productId") UUID productId, @Param("step") Step step);

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

}

// khiu hgujygugtuytutyuhyujgy kjhiuhyiu jhguyg hjgkyuyhh nhku nghuyg mnhkj
// hmgjh hjgjh hjgjhuiui uhiuoi uiuiui iu8iy787y8y7y7bu8u8kjjiji hjujioj hiuuji
// huhuihiuu kjhedioiorfuigutouiu uugtuiijuuiiuifvijhhhuuu mjjij hujujnj juiju
// huhuhhu hkuiuiu huiuiuuuuuuuuj hhui yugyu yuu yuy7uhyyuyuyhy njkui hiuyui
// huyhyu gyuy jkjui huj hukhju jujujj jjuouuujioihuyh yiyiu uiyhuiyh

// huiiui huiui huiyhiui hkuiuiour hukiiirfkuurfiuufrhrfhuhkhhgku

// iuuujioujio uhiiiouio8gutu8onjkhui