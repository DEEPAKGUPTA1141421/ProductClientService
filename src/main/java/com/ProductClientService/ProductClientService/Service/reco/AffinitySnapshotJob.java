package com.ProductClientService.ProductClientService.Service.reco;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nightly materialisation of user_affinity_snapshot from the 90d slice of
 * user_interaction_events. Runs in-database via a single SQL statement —
 * pulling 500M rows over JDBC would be ~two orders of magnitude slower
 * than letting Postgres aggregate in place.
 *
 * Scheduled at 02:00 IST (20:30 UTC) daily.
 *
 * NOTE: the table is created eagerly by JPA (ddl-auto=update) once the
 * UserAffinitySnapshot entity is scanned. This job assumes it exists.
 */
@Service
@Slf4j
public class AffinitySnapshotJob {

    @PersistenceContext
    private EntityManager em;

    @Scheduled(cron = "0 30 20 * * *", zone = "UTC")
    @Transactional
    public void refresh() {
        long started = System.currentTimeMillis();
        try {
            int rows = em.createNativeQuery("""
                WITH weighted AS (
                    SELECT uie.user_id,
                           p.category_id::text AS cat,
                           (CASE uie.event_type
                                WHEN 1 THEN 1 WHEN 2 THEN 2 WHEN 3 THEN 4
                                WHEN 4 THEN 6 WHEN 5 THEN 10 WHEN 6 THEN 14
                                ELSE 0
                            END)
                           * exp(-EXTRACT(EPOCH FROM (now() - uie.event_ts)) / (21.0 * 86400)) AS w,
                           uie.event_type
                    FROM user_interaction_events uie
                    JOIN products p ON p.id = uie.product_id
                    WHERE uie.event_ts > now() - INTERVAL '90 days'
                      AND uie.user_id IS NOT NULL
                ),
                per_cat AS (
                    SELECT user_id, cat, SUM(w) AS score,
                           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY SUM(w) DESC) AS rn
                    FROM weighted
                    GROUP BY user_id, cat
                ),
                cod AS (
                    SELECT user_id,
                           SUM(CASE WHEN event_type = 6 THEN 1 ELSE 0 END)::numeric
                           / NULLIF(SUM(CASE WHEN event_type IN (5,6) THEN 1 ELSE 0 END), 0) AS ratio
                    FROM weighted
                    GROUP BY user_id
                )
                INSERT INTO user_affinity_snapshot (user_id, top_categories, cod_ratio, meta, updated_at)
                SELECT pc.user_id,
                       jsonb_agg(jsonb_build_object('catId', pc.cat, 'score', pc.score)
                                 ORDER BY pc.score DESC)
                           FILTER (WHERE pc.rn <= 10) AS top_categories,
                       COALESCE(c.ratio, 0)::numeric(3,2),
                       '{}'::jsonb,
                       now()
                FROM per_cat pc
                LEFT JOIN cod c ON c.user_id = pc.user_id
                GROUP BY pc.user_id, c.ratio
                ON CONFLICT (user_id) DO UPDATE SET
                    top_categories = EXCLUDED.top_categories,
                    cod_ratio      = EXCLUDED.cod_ratio,
                    updated_at     = now()
            """).executeUpdate();

            log.info("AffinitySnapshotJob refreshed {} users in {} ms",
                    rows, System.currentTimeMillis() - started);
        } catch (Exception e) {
            log.error("AffinitySnapshotJob failed: {}", e.getMessage(), e);
        }
    }
}
