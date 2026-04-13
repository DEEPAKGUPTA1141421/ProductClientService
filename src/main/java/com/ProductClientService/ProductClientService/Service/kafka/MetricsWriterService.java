package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.Model.ProductMetrics;
import com.ProductClientService.ProductClientService.Repository.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * MetricsWriterService
 * ─────────────────────
 * Single entry point for writing to product_metrics.
 *
 * All writes are atomic SQL increments — no read-modify-write.
 * This means concurrent Kafka consumer threads can call these methods
 * simultaneously without any race condition or lost update.
 *
 * The ensure() helper creates a zero-row on first encounter so the UPDATE
 * always finds a row (INSERT-on-miss, then UPDATE).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsWriterService {

    private final ProductMetricsRepository repo;

    // ── Public increment API ──────────────────────────────────────────────────

    @Transactional
    public void recordView(UUID productId) {
        ensure(productId);
        repo.incrementViewCount(productId);
    }

    @Transactional
    public void recordCartAdd(UUID productId) {
        ensure(productId);
        repo.incrementCartAddCount(productId);
    }

    @Transactional
    public void recordWishlistAdd(UUID productId) {
        ensure(productId);
        repo.incrementWishlistCount(productId);
    }

    @Transactional
    public void recordWishlistRemove(UUID productId) {
        ensure(productId);
        repo.decrementWishlistCount(productId);
    }

    /**
     * Called once per order-line-item.
     * Increments purchases by qty, orders by 1, and both sales windows by qty.
     */
    @Transactional
    public void recordPurchase(UUID productId, int quantity) {
        ensure(productId);
        repo.incrementPurchase(productId, Math.max(1, quantity));
    }

    @Transactional
    public void recordReturn(UUID productId, int quantity) {
        ensure(productId);
        repo.incrementReturnCount(productId, Math.max(1, quantity));
    }

    @Transactional
    public void recordSearchImpression(UUID productId, long count) {
        ensure(productId);
        repo.incrementSearchImpressions(productId, count);
    }

    // ── Row-existence guard ───────────────────────────────────────────────────

    /**
     * Inserts a zero-metrics row for a product if one does not yet exist.
     * Uses save-if-absent pattern. The outer @Transactional ensures the check
     * and insert are atomic within this thread.
     */
    private void ensure(UUID productId) {
        if (!repo.existsById(productId)) {
            try {
                repo.save(new ProductMetrics(productId));
            } catch (Exception ignored) {
                // Another thread may have inserted concurrently — safe to swallow
            }
        }
    }
}
