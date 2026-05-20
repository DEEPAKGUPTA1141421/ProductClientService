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

    /**
     * Computes avg rating and total review count across all LIVE products
     * belonging to a seller. Used by ShopRatingUpdater to sync shops-v1 ES.
     * Returns [avgRating (Double), totalCount (Long)] — both null if no reviews.
     */
    @Query("SELECT AVG(r.rating), COUNT(r) FROM ProductRating r " +
           "WHERE r.product.seller.id = :sellerId AND r.product.step = 4")
    List<Object[]> findSellerRatingSummary(@Param("sellerId") UUID sellerId);

    /** Paginated reviews across all products of a seller, newest first. */
    @Query(value = """
            SELECT r.id               AS id,
                   r.rating           AS rating,
                   r.title            AS title,
                   r.review           AS review,
                   r.helpful_count    AS helpfulCount,
                   r.verified_purchase AS verifiedPurchase,
                   r.created_at       AS createdAt,
                   p.id               AS productId,
                   p.name             AS productName,
                   u.name             AS reviewerName
            FROM product_ratings r
            JOIN products p ON p.id = r.product_id
            JOIN users    u ON u.id = r.user_id
            WHERE p.seller_id = :sellerId
            ORDER BY r.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findReviewsBySeller(@Param("sellerId") UUID sellerId,
                                       @Param("size")     int  size,
                                       @Param("offset")   int  offset);

    @Query(value = "SELECT COUNT(*) FROM product_ratings r JOIN products p ON p.id = r.product_id WHERE p.seller_id = :sellerId",
           nativeQuery = true)
    long countReviewsBySeller(@Param("sellerId") UUID sellerId);

    /** Star (1-5) distribution for all seller's products. */
    @Query(value = """
            SELECT r.rating, COUNT(*) AS cnt
            FROM product_ratings r
            JOIN products p ON p.id = r.product_id
            WHERE p.seller_id = :sellerId
            GROUP BY r.rating
            ORDER BY r.rating DESC
            """, nativeQuery = true)
    List<Object[]> findSellerStarDistribution(@Param("sellerId") UUID sellerId);

    @Modifying
    @Query("UPDATE ProductRating r SET r.helpfulCount = r.helpfulCount + 1 WHERE r.id = :reviewId")
    void incrementHelpfulCount(@Param("reviewId") UUID reviewId);

    @Modifying
    @Query("UPDATE ProductRating r SET r.helpfulCount = GREATEST(r.helpfulCount - 1, 0) WHERE r.id = :reviewId")
    void decrementHelpfulCount(@Param("reviewId") UUID reviewId);
}
