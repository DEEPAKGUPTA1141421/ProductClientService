package com.ProductClientService.ProductClientService.Service.reco;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Deterministic user-bucketing for A/B experiments.
 * Hashing userId (not a random draw) means the same user stays on the same
 * variant across requests and devices — a hard requirement for experiment
 * integrity.
 *
 * Current split: 50/50. Adjust the threshold when ramping variant B.
 */
@Service
public class ExperimentRouter {

    private static final int BUCKET_COUNT = 100;
    private static final int VARIANT_B_THRESHOLD = 50;

    public String variantFor(UUID userId) {
        if (userId == null) return "A";
        int bucket = Math.floorMod(userId.hashCode(), BUCKET_COUNT);
        return bucket < VARIANT_B_THRESHOLD ? "A" : "B";
    }
}
