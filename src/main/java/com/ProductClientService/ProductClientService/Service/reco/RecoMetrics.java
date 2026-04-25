package com.ProductClientService.ProductClientService.Service.reco;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Dimensions chosen to match the alerts in ops/prometheus/alerts.yml.
 * Keep cardinality low: `variant` ∈ {A,B}, `context` ∈ {HOME,PDP,CART},
 * `path` ∈ {served,cold_start,cache_hit,empty,degraded}.
 */
@Component
public class RecoMetrics {

    private final MeterRegistry registry;

    // Reco
    public final Timer recoLatency;
    public final Counter recoCacheHit;
    public final Counter recoCacheMiss;
    public final Counter recoFallback;        // cold-start used because serving returned nothing
    public final Counter recoEmpty;           // even cold-start returned nothing
    public final Counter recoDegraded;        // served from pre-warm only, model skipped

    // Similarity
    public final Timer simLatency;
    public final Counter simCacheHit;
    public final Counter simEmpty;
    public final Counter simVectorPath;
    public final Counter simMltPath;

    public RecoMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.recoLatency = Timer.builder("reco_request_seconds")
                .description("End-to-end /reco/for-you latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
        this.recoCacheHit  = registry.counter("reco_cache_hit_total");
        this.recoCacheMiss = registry.counter("reco_cache_miss_total");
        this.recoFallback  = registry.counter("reco_fallback_total");
        this.recoEmpty     = registry.counter("reco_empty_result_total");
        this.recoDegraded  = registry.counter("reco_degraded_served_total");

        this.simLatency = Timer.builder("sim_request_seconds")
                .description("End-to-end /product/{id}/similar latency")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
        this.simCacheHit    = registry.counter("sim_cache_hit_total");
        this.simEmpty       = registry.counter("sim_empty_result_total");
        this.simVectorPath  = registry.counter("sim_path_total", "path", "knn");
        this.simMltPath     = registry.counter("sim_path_total", "path", "mlt");
    }

    public MeterRegistry registry() { return registry; }
}
