package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    // List<ProductVariant> findByProductId(UUID productId);
}
