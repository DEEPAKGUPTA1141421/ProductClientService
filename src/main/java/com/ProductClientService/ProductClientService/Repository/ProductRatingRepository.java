package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.ProductRating;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRating, UUID> {

    List<ProductRating> findByProductId(UUID productId);

    @Query("SELECT AVG(r.rating) FROM ProductRating r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(UUID productId);

    @Query("SELECT COUNT(r.id) FROM ProductRating r WHERE r.product.id = :productId")
    Long countRatingsByProductId(UUID productId);

    Optional<ProductRating> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(pr.rating), COUNT(pr) FROM ProductRating pr WHERE pr.product.id = :productId")
    Object[] findAvgAndCountByProductId(@Param("productId") UUID productId);
}
