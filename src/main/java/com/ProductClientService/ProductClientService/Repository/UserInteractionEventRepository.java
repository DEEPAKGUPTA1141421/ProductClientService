package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.UserInteractionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserInteractionEventRepository extends JpaRepository<UserInteractionEventEntity, Long> {

    /**
     * Weekly count of a given event type (e.g. VIEW) across a set of products
     * since a given instant — backs the seller "Product activity" table.
     * Returns rows of [weekStart (Postgres date_trunc('week', ...)), count].
     */
    @Query(value = """
            SELECT date_trunc('week', event_ts) AS week, COUNT(*) AS cnt
            FROM user_interaction_events
            WHERE event_type = :eventType
              AND product_id IN (:productIds)
              AND event_ts >= :since
            GROUP BY week
            ORDER BY week
            """, nativeQuery = true)
    List<Object[]> findWeeklyEventCounts(@Param("productIds") List<UUID> productIds,
                                          @Param("eventType") short eventType,
                                          @Param("since") Instant since);

    /**
     * Weekly count of distinct products with ANY interaction activity, since a
     * given instant — backs the "products" column of the "Product activity" table.
     */
    @Query(value = """
            SELECT date_trunc('week', event_ts) AS week, COUNT(DISTINCT product_id) AS cnt
            FROM user_interaction_events
            WHERE product_id IN (:productIds)
              AND event_ts >= :since
            GROUP BY week
            ORDER BY week
            """, nativeQuery = true)
    List<Object[]> findWeeklyActiveProductCounts(@Param("productIds") List<UUID> productIds,
                                                  @Param("since") Instant since);

    /** Interaction count grouped by source (home/search/pdp/push/cart), scoped to a seller's products. */
    @Query(value = """
            SELECT source, COUNT(*) AS cnt
            FROM user_interaction_events
            WHERE product_id IN (:productIds)
              AND event_ts >= :since
            GROUP BY source
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> findTrafficSourceCounts(@Param("productIds") List<UUID> productIds,
                                            @Param("since") Instant since);

    /** Distinct-session viewer count per product, scoped to a seller's products. */
    @Query(value = """
            SELECT product_id, COUNT(DISTINCT session_id) AS cnt
            FROM user_interaction_events
            WHERE event_type = :eventType
              AND product_id IN (:productIds)
              AND event_ts >= :since
            GROUP BY product_id
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> findViewerCountsByProduct(@Param("productIds") List<UUID> productIds,
                                              @Param("eventType") short eventType,
                                              @Param("since") Instant since);

    /**
     * Top cities interacting with a seller's products, keyed off each user's
     * default address — real customer-location data (no geo-IP), scoped to
     * India-only addresses so this reads as "top cities" rather than "top
     * countries" (every address.country is currently "IN").
     */
    @Query(value = """
            SELECT a.city, COUNT(DISTINCT uie.session_id) AS cnt
            FROM user_interaction_events uie
            JOIN addresses a ON a.user_id = uie.user_id AND a.is_default = true
            WHERE uie.product_id IN (:productIds)
              AND uie.event_ts >= :since
            GROUP BY a.city
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopCities(@Param("productIds") List<UUID> productIds,
                                  @Param("since") Instant since,
                                  @Param("limit") int limit);

    /** Monthly VIEW counts across a seller's products — backs the "Product views" chart. */
    @Query(value = """
            SELECT date_trunc('month', event_ts) AS month, COUNT(*) AS cnt
            FROM user_interaction_events
            WHERE event_type = :eventType
              AND product_id IN (:productIds)
              AND event_ts >= :since
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> findMonthlyViewCounts(@Param("productIds") List<UUID> productIds,
                                          @Param("eventType") short eventType,
                                          @Param("since") Instant since);
}
