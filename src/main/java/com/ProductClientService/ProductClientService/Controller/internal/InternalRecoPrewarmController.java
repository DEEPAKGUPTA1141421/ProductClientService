package com.ProductClientService.ProductClientService.Controller.internal;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.reco.RecoContext;
import com.ProductClientService.ProductClientService.Service.reco.RecoOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Called by the sale-day pre-warm Airflow DAG 6h before traffic spikes.
 * Iterates over the top 2M MAU and primes Redis so that incoming /reco
 * requests are cache hits. While pre-warming the orchestrator still runs
 * in full mode — degradedMode should remain OFF during warm-up so fresh
 * entries are real (not empty placeholders).
 *
 * Secured by InternalApiKeyFilter (path matches /internal/**).
 */
@RestController
@RequestMapping("/internal/v1/reco")
@RequiredArgsConstructor
@Slf4j
public class InternalRecoPrewarmController {

    private static final int BATCH_CAP = 5_000;

    private final RecoOrchestrator orchestrator;

    @PostMapping("/prewarm")
    public ResponseEntity<ApiResponse<Object>> prewarm(@RequestBody PrewarmRequest req) {
        if (req == null || req.userIds == null || req.userIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "userIds required", null, 400));
        }
        if (req.userIds.size() > BATCH_CAP) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "batch exceeds " + BATCH_CAP, null, 400));
        }
        RecoContext ctx = RecoContext.parse(req.context);
        int k = req.k > 0 ? req.k : 20;
        warmAsync(req.userIds, ctx, k);
        return ResponseEntity.accepted()
                .body(new ApiResponse<>(true, "prewarm scheduled",
                        Map.of("count", req.userIds.size()), 202));
    }

    @Async
    void warmAsync(List<UUID> userIds, RecoContext ctx, int k) {
        long started = System.currentTimeMillis();
        int ok = 0;
        for (UUID uid : userIds) {
            try {
                orchestrator.forYou(uid, k, ctx, null);
                ok++;
            } catch (Exception e) {
                log.debug("prewarm skipped for userId={}: {}", uid, e.getMessage());
            }
        }
        log.info("prewarm completed: {}/{} users in {} ms",
                ok, userIds.size(), System.currentTimeMillis() - started);
    }

    public static class PrewarmRequest {
        public List<UUID> userIds;
        public String context;
        public int k;
    }
}
