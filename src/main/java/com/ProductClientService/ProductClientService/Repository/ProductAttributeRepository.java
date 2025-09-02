package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ProductClientService.ProductClientService.Model.ProductAttribute;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    @Query(value = "SELECT pa.* " +
            "FROM product_attributes pa " +
            "JOIN attributes a ON pa.attribute_id = a.id " +
            "WHERE pa.product_id = :productId", nativeQuery = true)
    List<ProductAttribute> findByProductIdWithAttribute(@Param("productId") UUID productId);
}
