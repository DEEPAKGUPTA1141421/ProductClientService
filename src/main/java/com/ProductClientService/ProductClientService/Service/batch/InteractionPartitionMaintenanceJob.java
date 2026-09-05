package com.ProductClientService.ProductClientService.Service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * InteractionPartitionMaintenanceJob
 * ───────────────────────────────────
 * user_interaction_events is RANGE-partitioned by month (see
 * V20__interaction_events.sql). Postgres does not auto-create partitions —
 * an insert whose event_ts falls outside every existing partition fails
 * with "no partition of relation ... found for row" (this is what caused
 * inserts to fail from July 2026 onward, since V20 only bootstrapped
 * through June 2026).
 *
 * This job keeps 3 months of partitions ready ahead of the current month so
 * the table never runs dry again. It's idempotent (CREATE TABLE IF NOT
 * EXISTS) and safe to run repeatedly. A DEFAULT partition (added in V39)
 * remains as a last-resort safety net if this job is ever paused.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionPartitionMaintenanceJob {

    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final int MONTHS_AHEAD = 3;

    private final JdbcTemplate jdbcTemplate;

    /** Runs daily at 00:05 IST = 18:35 UTC — cron is in server timezone (UTC). */
    @Scheduled(cron = "0 35 18 * * *")
    public void ensureFuturePartitions() {
        YearMonth current = YearMonth.from(LocalDate.now());
        for (int i = 0; i <= MONTHS_AHEAD; i++) {
            createPartitionIfMissing(current.plusMonths(i));
        }
    }

    private void createPartitionIfMissing(YearMonth month) {
        String suffix = month.format(SUFFIX);
        String tableName = "user_interaction_events_" + suffix;
        String from = month.atDay(1).toString();
        String to = month.plusMonths(1).atDay(1).toString();

        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF user_interaction_events " +
                        "FOR VALUES FROM ('%s') TO ('%s')",
                tableName, from, to);
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.error("InteractionPartitionMaintenanceJob: failed to ensure partition {}", tableName, e);
        }
    }
}
