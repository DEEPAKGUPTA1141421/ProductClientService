package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Page<ReturnRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<ReturnRequest> findByBookingIdAndUserId(String bookingId, UUID userId);

    boolean existsByBookingIdAndUserId(String bookingId, UUID userId);

    Page<ReturnRequest> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    /** "Open" = not yet terminally resolved (rejected/refunded are terminal). */
    @Query("SELECT COUNT(r) FROM ReturnRequest r WHERE r.sellerId = :sellerId " +
           "AND r.status NOT IN (com.ProductClientService.ProductClientService.Model.ReturnRequest.ReturnStatus.REJECTED, " +
           "com.ProductClientService.ProductClientService.Model.ReturnRequest.ReturnStatus.REFUNDED)")
    long countOpenBySellerId(@Param("sellerId") UUID sellerId);

    long countBySellerIdAndCreatedAtAfter(UUID sellerId, ZonedDateTime after);

    /**
     * Paginated refund/return requests for a seller's own products, enriched with the
     * product (name/category/cover image) and customer (name/avatar) details needed by
     * the "Refund requests" table — mirrors the join pattern used for seller reviews.
     */
    @Query(value = """
            SELECT r.id                AS id,
                   r.booking_id        AS bookingId,
                   r.status            AS status,
                   r.reason            AS reason,
                   r.description       AS description,
                   r.created_at        AS createdAt,
                   p.id                AS productId,
                   p.name              AS productName,
                   c.name              AS categoryName,
                   pm.url              AS productImageUrl,
                   u.id                AS customerId,
                   u.name              AS customerName,
                   u.avatar_url        AS customerAvatarUrl
            FROM return_requests r
            LEFT JOIN products      p  ON p.id = r.product_id
            LEFT JOIN categories    c  ON c.id = p.category_id
            LEFT JOIN product_media pm ON pm.product_id = p.id AND pm.is_cover = true
            JOIN users u ON u.id = r.user_id
            WHERE r.seller_id = :sellerId
              AND r.status IN (:statuses)
            ORDER BY r.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM return_requests r
            WHERE r.seller_id = :sellerId AND r.status IN (:statuses)
            """,
            nativeQuery = true)
    Page<Object[]> findSellerReturnsDetailed(
            @Param("sellerId")  UUID sellerId,
            @Param("statuses")  List<String> statuses,
            Pageable pageable);
}
