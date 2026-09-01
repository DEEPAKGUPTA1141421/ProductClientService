package com.ProductClientService.ProductClientService.Service.seller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.ProductClientService.ProductClientService.Service.ElasticsearchProductIndexer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DiscountScheduleService
 * ────────────────────────
 * Sweeps variants with a scheduled (start/end windowed) discount every 5
 * minutes and re-syncs the buyer-facing discount_price/discount_percentage
 * columns to match the discount's currently-effective state — a window that
 * just started needs the columns populated, one that just ended needs them
 * cleared. Only touches variants that actually have a schedule; a discount
 * with no start/end window is already correct as of its last write.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountScheduleService {

    private final ProductVariantRepository productVariantRepository;
    private final ElasticsearchProductIndexer elasticsearchProductIndexer;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void syncScheduledDiscounts() {
        List<ProductVariant> variants = productVariantRepository.findScheduledDiscountVariants();
        if (variants.isEmpty()) {
            return;
        }

        java.util.Set<UUID> productIdsToReindex = new java.util.LinkedHashSet<>();

        for (ProductVariant variant : variants) {
            boolean wasEffective = variant.getDiscountPrice() != null;
            variant.recomputeEffectiveDiscount();
            boolean isEffective = variant.getDiscountPrice() != null;
            productVariantRepository.save(variant);

            if (wasEffective != isEffective && variant.getProduct() != null) {
                productIdsToReindex.add(variant.getProduct().getId());
            }
        }

        for (UUID productId : productIdsToReindex) {
            CompletableFuture.runAsync(() -> {
                try {
                    elasticsearchProductIndexer.indexProduct(productId);
                } catch (Exception e) {
                    log.warn("ES re-index failed during discount sweep for productId={}: {}", productId,
                            e.getMessage());
                }
            });
        }

        log.info("Discount schedule sweep: checked {} variant(s), reindexed {} product(s)",
                variants.size(), productIdsToReindex.size());
    }
}
