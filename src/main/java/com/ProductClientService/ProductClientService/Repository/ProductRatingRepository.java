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

    @Query(value = """
            SELECT
                COALESCE(AVG(r.rating), 0) AS average_rating,
                COUNT(r.id) AS total_ratings,
                COUNT(r.id) FILTER (WHERE r.rating = 5) AS five_star,
                COUNT(r.id) FILTER (WHERE r.rating = 4) AS four_star,
                COUNT(r.id) FILTER (WHERE r.rating = 3) AS three_star,
                COUNT(r.id) FILTER (WHERE r.rating = 2) AS two_star,
                COUNT(r.id) FILTER (WHERE r.rating = 1) AS one_star,
                COUNT(r.id) FILTER (WHERE r.verified_purchase = true) AS verified_count,
                (
                    SELECT COUNT(DISTINCT ri.review_id)
                    FROM review_images ri
                    JOIN product_ratings pr ON pr.id = ri.review_id
                    WHERE pr.product_id = :productId
                ) AS with_images_count
            FROM product_ratings r
            WHERE r.product_id = :productId
            """, nativeQuery = true)
    Object[] findRatingSummaryRowByProductId(@Param("productId") UUID productId);

    /**
     * Computes avg rating and total review count across all LIVE products
     * belonging to a seller. Used by ShopRatingUpdater to sync shops-v1 ES.
     * Returns [avgRating (Double), totalCount (Long)] — both null if no reviews.
     */
    @Query("SELECT AVG(r.rating), COUNT(r) FROM ProductRating r " +
           "WHERE r.product.seller.id = :sellerId AND r.product.step = 4")
    List<Object[]> findSellerRatingSummary(@Param("sellerId") UUID sellerId);

    /** Paginated reviews (comments) across all products of a seller, newest first, optionally filtered by product name. */
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
                   c.name             AS categoryName,
                   pm.url             AS productImageUrl,
                   u.id               AS reviewerId,
                   u.name             AS reviewerName,
                   u.avatar_url       AS reviewerAvatarUrl,
                   r.seller_reply     AS sellerReply,
                   r.seller_reply_at  AS sellerReplyAt,
                   r.seller_reaction  AS sellerReaction
            FROM product_ratings r
            JOIN products   p ON p.id = r.product_id
            JOIN users      u ON u.id = r.user_id
            LEFT JOIN categories c ON c.id = p.category_id
            LEFT JOIN product_media pm ON pm.product_id = p.id AND pm.is_cover = true
            WHERE p.seller_id = :sellerId
              AND (:query IS NULL OR p.name ILIKE CONCAT('%', :query, '%'))
            ORDER BY r.created_at DESC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findReviewsBySeller(@Param("sellerId") UUID sellerId,
                                       @Param("query")    String query,
                                       @Param("size")     int  size,
                                       @Param("offset")   int  offset);

    @Query(value = """
            SELECT COUNT(*) FROM product_ratings r
            JOIN products p ON p.id = r.product_id
            WHERE p.seller_id = :sellerId
              AND (:query IS NULL OR p.name ILIKE CONCAT('%', :query, '%'))
            """, nativeQuery = true)
    long countReviewsBySeller(@Param("sellerId") UUID sellerId, @Param("query") String query);

    /** True when the given review belongs to a product owned by the given seller — used to authorize moderation actions. */
    @Query(value = """
            SELECT COUNT(*) > 0 FROM product_ratings r
            JOIN products p ON p.id = r.product_id
            WHERE r.id = :reviewId AND p.seller_id = :sellerId
            """, nativeQuery = true)
    boolean existsByIdAndSellerId(@Param("reviewId") UUID reviewId, @Param("sellerId") UUID sellerId);

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

    /**
     * Weekly review ("comments") counts across all of a seller's products since
     * a given timestamp — backs the "Product activity" table's comments column.
     * Returns rows of [weekStart (Postgres date_trunc('week', ...)), count].
     */
    @Query(value = """
            SELECT date_trunc('week', r.created_at) AS week, COUNT(*) AS cnt
            FROM product_ratings r
            JOIN products p ON p.id = r.product_id
            WHERE p.seller_id = :sellerId
              AND r.created_at >= :since
            GROUP BY week
            ORDER BY week
            """, nativeQuery = true)
    List<Object[]> findWeeklyReviewCountsBySeller(@Param("sellerId") UUID sellerId,
                                                   @Param("since") java.time.ZonedDateTime since);
}
