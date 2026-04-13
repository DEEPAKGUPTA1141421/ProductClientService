package com.ProductClientService.ProductClientService.Service.batch;

import com.ProductClientService.ProductClientService.Repository.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * RecentSalesResetJob
 * ────────────────────
 * Runs daily at midnight IST.
 *
 * In a single atomic UPDATE:
 *   recent_sales_30d += recent_sales_7d   (accumulate into 30-day window)
 *   recent_sales_7d   = 0                 (reset the 7-day counter)
 *
 * This keeps the recency signal in ProductRankingScorerJob fresh.
 * Without this reset, recent_sales_7d would grow unbounded and lose its
 * "trending right now" meaning.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecentSalesResetJob {

    private final ProductMetricsRepository metricsRepository;

    /** 00:00 IST = 18:30 UTC — cron is always in server timezone (UTC here). */
    @Scheduled(cron = "0 30 18 * * *")
    @Transactional
    public void run() {
        log.info("RecentSalesResetJob: rolling recent_sales_7d into recent_sales_30d");
        metricsRepository.rollAndResetRecentSales();
        log.info("RecentSalesResetJob: done");
    }
}
