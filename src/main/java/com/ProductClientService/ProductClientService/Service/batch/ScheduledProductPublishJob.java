package com.ProductClientService.ProductClientService.Service.batch;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ScheduledProductPublishJob
 * ──────────────────────────
 * Sweeps every minute for products whose seller-chosen scheduledAt has
 * arrived and publishes them via the same path as a manual "Publish now"
 * (SellerService#MakeProductLive), which also clears scheduledAt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledProductPublishJob {

    private final ProductRepository productRepository;
    private final SellerService sellerService;

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        List<UUID> dueIds = productRepository.findDueScheduledProductIds(now);
        if (dueIds.isEmpty()) return;

        log.info("ScheduledProductPublishJob: publishing {} due product(s)", dueIds.size());
        for (UUID productId : dueIds) {
            try {
                sellerService.MakeProductLive(productId);
            } catch (Exception e) {
                log.error("ScheduledProductPublishJob: failed to publish productId={}", productId, e);
            }
        }
    }
}
