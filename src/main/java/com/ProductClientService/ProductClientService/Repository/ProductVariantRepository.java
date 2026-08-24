package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

import com.ProductClientService.ProductClientService.Model.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM ProductVariant v " +
            "WHERE v.id = :variantId AND v.product.id = :productId AND v.product.seller.id = :sellerId")
    boolean existsByIdAndProductIdAndSellerId(
            @Param("variantId") UUID variantId,
            @Param("productId") UUID productId,
            @Param("sellerId") UUID sellerId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product_variants
            SET price = COALESCE(CAST(:price AS text), price),
                mrp = CASE
                    WHEN :price IS NULL THEN mrp
                    WHEN mrp IS NULL OR mrp = '' THEN CAST(:price AS text)
                    WHEN CAST(mrp AS numeric) < :price THEN CAST(:price AS text)
                    ELSE mrp
                END,
                stock = COALESCE(:stock, stock)
            WHERE product_id = :productId
            """, nativeQuery = true)
    int updateAllByProductId(
            @Param("productId") UUID productId,
            @Param("price") Long price,
            @Param("stock") Integer stock);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE product_variants
            SET price = COALESCE(CAST(:price AS text), price),
                mrp = CASE
                    WHEN :price IS NULL THEN mrp
                    WHEN mrp IS NULL OR mrp = '' THEN CAST(:price AS text)
                    WHEN CAST(mrp AS numeric) < :price THEN CAST(:price AS text)
                    ELSE mrp
                END,
                stock = COALESCE(:stock, stock)
            WHERE id = :variantId
              AND product_id = :productId
            """, nativeQuery = true)
    int updateByIdAndProductId(
            @Param("variantId") UUID variantId,
            @Param("productId") UUID productId,
            @Param("price") Long price,
            @Param("stock") Integer stock);
}
