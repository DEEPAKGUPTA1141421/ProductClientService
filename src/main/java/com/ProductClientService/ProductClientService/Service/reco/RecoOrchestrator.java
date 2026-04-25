package com.ProductClientService.ProductClientService.Service.reco;

import com.ProductClientService.ProductClientService.DTO.reco.RecoContext;
import com.ProductClientService.ProductClientService.DTO.reco.RecoItemDto;
import com.ProductClientService.ProductClientService.DTO.reco.RecoResponseDto;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import com.ProductClientService.ProductClientService.Service.session.SessionClickBufferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Single entry point for /reco/for-you.
 *
 * Request path:
 *   1. Hit Redis cache (reco:user:{userId}:v{model})
 *   2. Else call reco-serving (LightFM + XGBoost via RecoServingClient)
 *   3. Fallback: ColdStartResolver (session-aware popularity)
 *   4. Hydrate via ES mget → compact DTOs
 *   5. Cache 15m and return
 *
 * Because the reco-serving client fails silently on timeout/circuit-open,
 * this method never raises for a downstream outage. Empty results are
 * served before errors.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoOrchestrator {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final int MAX_K = 50;

    private final RecoServingClient servingClient;
    private final FeatureHydrator hydrator;
    private final ColdStartResolver coldStart;
    private final ExperimentRouter experimentRouter;
    private final SessionClickBufferService sessionBuffer;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final EventPublisherService publisher;
    private final RecoMetrics metrics;

    @Value("${reco.degradedMode:false}")
    private boolean degradedMode;

    public RecoResponseDto forYou(UUID userId, int requestedK, RecoContext context, UUID sessionId) {
        Timer.Sample sample = Timer.start(metrics.registry());
        try {
            return forYouInner(userId, requestedK, context, sessionId);
        } finally {
            sample.stop(metrics.recoLatency);
        }
    }

    private RecoResponseDto forYouInner(UUID userId, int requestedK, RecoContext context, UUID sessionId) {
        int k = Math.min(Math.max(requestedK, 1), MAX_K);
        String variant = experimentRouter.variantFor(userId);
        String cacheKey = "reco:user:" + userId + ":" + context + ":" + variant;

        RecoResponseDto cached = readCache(cacheKey);
        if (cached != null) {
            metrics.recoCacheHit.increment();
            return cached;
        }
        metrics.recoCacheMiss.increment();

        // Degraded mode: serve pre-warmed cache only. Miss = empty rather than
        // putting extra load on the model during a sale-day incident.
        if (degradedMode) {
            metrics.recoDegraded.increment();
            metrics.recoEmpty.increment();
            return RecoResponseDto.builder()
                    .recoId(UUID.randomUUID().toString())
                    .userId(userId.toString())
                    .modelVersion("degraded")
                    .experimentVariant(variant)
                    .context(context)
                    .coldStart(true)
                    .items(List.of())
                    .build();
        }

        List<String> sessionClicks = sessionBuffer.recentClicks(sessionId, 20);

        // 1. Personalised path
        Optional<RecoServingClient.Response> model =
                servingClient.recommend(userId, k, context.name(), sessionClicks);

        List<String> ids;
        Map<String, Double> scores = new HashMap<>();
        Map<String, String> reasons = new HashMap<>();
        String modelVersion;
        boolean coldFlag = false;

        if (model.isPresent() && !model.get().candidates().isEmpty()) {
            ids = new ArrayList<>(model.get().candidates().size());
            for (var c : model.get().candidates()) {
                ids.add(c.productId());
                scores.put(c.productId(), c.score());
                if (c.reason() != null) reasons.put(c.productId(), c.reason());
            }
            modelVersion = model.get().modelVersion();
        } else {
            // 2. Cold-start / fallback path
            metrics.recoFallback.increment();
            ColdStartResolver.Candidates cs = coldStart.resolve(k, sessionClicks);
            ids = cs.productIds;
            scores.putAll(cs.scores);
            reasons.putAll(cs.reasons);
            modelVersion = cs.modelVersion;
            coldFlag = true;
        }

        List<RecoItemDto> items = hydrator.hydrate(ids, scores, reasons);
        if (items.isEmpty()) metrics.recoEmpty.increment();

        RecoResponseDto resp = RecoResponseDto.builder()
                .recoId(UUID.randomUUID().toString())
                .userId(userId.toString())
                .modelVersion(modelVersion)
                .experimentVariant(variant)
                .context(context)
                .coldStart(coldFlag)
                .items(items)
                .build();

        writeCache(cacheKey, resp);
        return resp;
    }

    /** Fire-and-forget: attribute a click/dismiss/purchase back to a rendered reco. */
    public void recordFeedback(String recoId, UUID userId, UUID productId, String action) {
        try {
            publisher.publishInteractionBatch(List.of(
                com.ProductClientService.ProductClientService.DTO.events.UserInteractionEvent.builder()
                    .userId(userId)
                    .productId(productId)
                    .eventType(mapAction(action))
                    .source("reco:" + recoId)
                    .ts(java.time.Instant.now())
                    .build()));
        } catch (Exception e) {
            log.debug("reco feedback publish skipped: {}", e.getMessage());
        }
    }

    private com.ProductClientService.ProductClientService.DTO.events.InteractionType mapAction(String action) {
        return switch (action == null ? "" : action.toUpperCase()) {
            case "CLICK"    -> com.ProductClientService.ProductClientService.DTO.events.InteractionType.CLICK;
            case "PURCHASE" -> com.ProductClientService.ProductClientService.DTO.events.InteractionType.PURCHASE_PREPAID;
            default         -> com.ProductClientService.ProductClientService.DTO.events.InteractionType.VIEW;
        };
    }

    private RecoResponseDto readCache(String key) {
        try {
            String raw = redis.opsForValue().get(key);
            return raw != null ? mapper.readValue(raw, RecoResponseDto.class) : null;
        } catch (Exception e) { return null; }
    }

    private void writeCache(String key, RecoResponseDto resp) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(resp), CACHE_TTL);
        } catch (Exception e) {
            log.debug("reco cache write skipped: {}", e.getMessage());
        }
    }
}
