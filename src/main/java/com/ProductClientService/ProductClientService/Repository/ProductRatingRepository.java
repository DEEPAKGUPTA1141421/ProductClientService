package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    Page<ProductRating> findByProductId(UUID productId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM ProductRating r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(UUID productId);

    @Query("SELECT COUNT(r.id) FROM ProductRating r WHERE r.product.id = :productId")
    Long countRatingsByProductId(UUID productId);

    Optional<ProductRating> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM ProductRating r WHERE r.product.id = :productId")
    List<Object[]> findAvgAndCountByProductId(@Param("productId") UUID productId);

    /** Returns count per star (1-5) for the rating distribution histogram. */
    @Query("SELECT r.rating, COUNT(r) FROM ProductRating r WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> findStarDistributionByProductId(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE ProductRating r SET r.helpfulCount = r.helpfulCount + 1 WHERE r.id = :reviewId")
    void incrementHelpfulCount(@Param("reviewId") UUID reviewId);

    @Modifying
    @Query("UPDATE ProductRating r SET r.helpfulCount = GREATEST(r.helpfulCount - 1, 0) WHERE r.id = :reviewId")
    void decrementHelpfulCount(@Param("reviewId") UUID reviewId);
}
