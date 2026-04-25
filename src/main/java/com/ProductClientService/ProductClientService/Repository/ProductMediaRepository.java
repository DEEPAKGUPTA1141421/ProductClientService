package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {

    List<ProductMedia> findByProductIdOrderByPositionAsc(UUID productId);

    int countByProductId(UUID productId);

    @Modifying
    @Query("UPDATE ProductMedia m SET m.isCover = false WHERE m.product.id = :productId")
    void clearCoverForProduct(@Param("productId") UUID productId);

    @Query("SELECT COALESCE(MAX(m.position), -1) FROM ProductMedia m WHERE m.product.id = :productId")
    int findMaxPositionByProductId(@Param("productId") UUID productId);
}
