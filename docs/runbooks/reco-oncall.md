# Reco on-call runbook

Paging alerts route here. Keep this file under 200 lines — oncall scrolls
past everything else during an incident.

## Quick links

- Grafana: `grafana.internal/d/reco-overview`
- Service: `ProductClientService` (Spring Boot, port 8081)
- Reco-serving (Python): `reco-serving`, port 8100
- Metrics endpoint: `/actuator/prometheus`
- Feature flags: `reco.degradedMode`, `reco.serving.enabled`

---

## #p99-breach  —  RecoP99LatencyBreached

**Likely causes** (most → least common)
1. reco-serving pod GC pause or OOM → reco_fallback_total spikes.
2. Redis cluster slot migration in progress → cache miss ratio jumps.
3. Elasticsearch hot shard (single category trending) → mget stalls.

**Triage**
1. `kubectl -n reco top pods` — any pod near memory limit? Restart it.
2. Check `reco_fallback_total` rate. Non-zero → reco-serving is the cause.
3. Check `reco_cache_hit_ratio` — if <70% the pre-warm DAG probably failed.
   Re-run `sale_prewarmer_dag` manually for top 500k MAU.

**Mitigation** (order of escalation)
- Scale reco-serving: `kubectl -n reco scale deploy reco-serving --replicas=8`
- Flip degraded mode: patch config-map `reco-config` → `reco.degradedMode=true`
  then rolling-restart `product-client-service`. Traffic now served from
  Redis only; empty for users without pre-warmed entries. Acceptable for
  30 min, not longer — degraded mode alert fires after that.

---

## #sim-p99-breach  —  SimP99LatencyBreached

**Likely causes**
1. ES `text_embedding` field mapping change being reindexed.
2. kNN num_candidates too high on a hot product.

**Triage**
- Check ES tasks: `GET _tasks?actions=*search*&detailed`.
- If `reco.similarity.vectorsEnabled=true` and the incident started after
  a model bump, flip the flag back to false. `more_like_this` is ~2× slower
  on average but has no vector cache warm-up cost.

---

## #drift  —  RecoRecallDrift

Nightly `reco_train_lightfm_dag` regression > 10% vs 7d mean. This is NOT
a page-now situation unless CTR has already dropped. Steps:

1. Pull the training run's MLflow artefact — check if the interaction
   counts jumped (usually a tracking double-send bug in `InteractionController`).
2. If legitimate drift, bump `reco_retrain_emergency` with double the
   training window.

---

## Flipping degraded mode

```bash
kubectl -n reco patch cm reco-config \
  --type merge -p '{"data":{"reco.degradedMode":"true"}}'
kubectl -n reco rollout restart deploy product-client-service
```

Verify `reco_degraded_served_total` starts incrementing within 30s.

**Reverting**: same patch with `"false"` + rollout restart. Once safe,
re-run the pre-warm DAG to refill Redis before traffic normalises — stale
entries can produce CTR drops even after the incident is resolved.

---

## Pre-warm DAG

Airflow DAG `sale_prewarmer_dag` (in `reco-serving/dags/`) hits
`POST /internal/v1/reco/prewarm` with 5k-user batches. Before a sale:

- Ensure DAG has completed at least 4h before traffic ramp.
- `reco_cache_hit_ratio` should be ≥90% within 1h of DAG completion.
- Redis memory usage should plateau below 70% of configured max.
