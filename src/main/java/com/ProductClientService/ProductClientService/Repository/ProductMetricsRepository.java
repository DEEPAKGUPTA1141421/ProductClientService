package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, UUID> {

    // ── Atomic single-column increments (no read-modify-write) ───────────────
    // Each UPDATE touches exactly one row and one column → no lock contention.

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.viewCount = m.viewCount + 1 WHERE m.productId = :id")
    void incrementViewCount(@Param("id") UUID productId);

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.searchImpressionCount = m.searchImpressionCount + :n WHERE m.productId = :id")
    void incrementSearchImpressions(@Param("id") UUID productId, @Param("n") long n);

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.cartAddCount = m.cartAddCount + 1 WHERE m.productId = :id")
    void incrementCartAddCount(@Param("id") UUID productId);

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.wishlistCount = m.wishlistCount + 1 WHERE m.productId = :id")
    void incrementWishlistCount(@Param("id") UUID productId);

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.wishlistCount = GREATEST(m.wishlistCount - 1, 0) WHERE m.productId = :id")
    void decrementWishlistCount(@Param("id") UUID productId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ProductMetrics m
            SET m.numberOfPurchases = m.numberOfPurchases + :qty,
                m.numberOfOrders    = m.numberOfOrders    + 1,
                m.recentSales7d     = m.recentSales7d     + :qty,
                m.recentSales30d    = m.recentSales30d    + :qty
            WHERE m.productId = :id
            """)
    void incrementPurchase(@Param("id") UUID productId, @Param("qty") int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.returnCount = m.returnCount + :qty WHERE m.productId = :id")
    void incrementReturnCount(@Param("id") UUID productId, @Param("qty") int quantity);

    // ── Batch scorer helpers ──────────────────────────────────────────────────

    /** Load all metrics for scoring. Runs in the batch scorer every 6 h. */
    @Query("SELECT m FROM ProductMetrics m")
    List<ProductMetrics> findAllForScoring();

    @Modifying
    @Transactional
    @Query("""
            UPDATE ProductMetrics m
            SET m.conversionRate  = :convRate,
                m.clickThroughRate = :ctr,
                m.returnRate       = :retRate,
                m.rankingScore     = :score,
                m.lastScoredAt     = CURRENT_TIMESTAMP
            WHERE m.productId = :id
            """)
    void updateComputedFields(
            @Param("id") UUID productId,
            @Param("convRate") double conversionRate,
            @Param("ctr") double clickThroughRate,
            @Param("retRate") double returnRate,
            @Param("score") double rankingScore);

    // ── Daily reset (RecentSalesResetJob) ─────────────────────────────────────

    @Modifying
    @Transactional
    @Query("UPDATE ProductMetrics m SET m.recentSales30d = m.recentSales30d + m.recentSales7d, m.recentSales7d = 0")
    void rollAndResetRecentSales();
}
