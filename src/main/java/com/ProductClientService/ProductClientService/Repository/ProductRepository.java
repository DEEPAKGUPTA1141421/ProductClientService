package com.ProductClientService.ProductClientService.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.Product.Step;

import jakarta.transaction.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
        @Modifying
        @Transactional
        @Query("UPDATE Product p SET p.step = :step WHERE p.id = :productId")
        int updateStatusById(@Param("productId") UUID productId, @Param("step") Step step);

        @Query("SELECT DISTINCT p FROM Product p " +
                        "LEFT JOIN FETCH p.productAttributes pa " +
                        "LEFT JOIN FETCH pa.variants " +
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
}

// nbjhckhukjn njjfonihiufuhnijfiu