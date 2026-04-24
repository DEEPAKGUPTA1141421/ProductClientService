# reco-serving

Python microservice responsible for:
1. **Offline embedding generation** — consumes `product.live` and `product.upserted`
   from Kafka, runs `intfloat/multilingual-e5-base` (384 dims) for text and
   OpenCLIP ViT-B/32 (512 dims) for images, writes vectors into the
   `products-v1` Elasticsearch index (fields `text_embedding`,
   `image_embedding`). Also writes to Milvus once SKU count > 2M.
2. **Online recommendation serving** (Phase 3) — gRPC service backing
   `/api/v1/reco/for-you`. Not in scope for Phase 2.

## Contract with ProductClientService (Java)

Elasticsearch mapping is owned by
`ElasticsearchIndexInitializer.patchProductsIndexForSimilarity()` on the Java
side — do **not** recreate fields here, just write values.

Fields to populate per product doc:
- `text_embedding`  : `float[384]`
- `image_embedding` : `float[512]`
- `popularity_7d`   : `float` (rolling 7d view/purchase score, rank_feature)

Once the first backfill completes, flip the Java config:
```
reco.similarity.vectorsEnabled=true
reco.similarity.textModelVersion=e5_v1
```
Java will switch from `more_like_this` to kNN automatically.

## Layout (to be implemented)

```
reco-serving/
  app/
    embedding_worker.py      # Kafka consumer → embed → ES bulk update
    serving.py               # FastAPI + gRPC (Phase 3)
  dags/
    product_embedding_dag.py # Airflow: nightly backfill of stale vectors
    reco_train_lightfm_dag.py
  proto/
    reco.proto
  Dockerfile
  requirements.txt
```

## Key dependencies

```
fastapi==0.110.1
grpcio==1.62.2
sentence-transformers==2.7.0   # multilingual-e5-base
open-clip-torch==2.24.0        # ViT-B/32
elasticsearch==8.13.0
pymilvus==2.4.0
kafka-python==2.0.2
```

## Kafka topics consumed

| topic                | payload                                  |
|----------------------|------------------------------------------|
| product.live         | `{productId}`                            |
| product.upserted     | `{productId, version}` (to be added)     |
| user.interaction     | for popularity_7d rollup (Phase 4)       |

## Acceptance criteria (Phase 2)

- Batch backfill of 500k SKUs completes in <6h on 4× m5.2xlarge
- ES `text_embedding` field populated on ≥99% of LIVE products within 24h of
  a new upload
- Hindi-only product titles retrieve Hindi neighbours (manual eval recall
  ≥0.6 on 50 labelled pairs)
