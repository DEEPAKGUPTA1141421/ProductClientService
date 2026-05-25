package com.ProductClientService.ProductClientService.Service.seller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.ProductClientService.ProductClientService.Configuration.ElasticsearchIndexInitializer;
import com.ProductClientService.ProductClientService.DTO.search.StandardCatalogDocument;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * StandardProductIndexer
 * ──────────────────────
 * Writes StandardProduct catalog entries to the "catalog-v1" Elasticsearch index.
 *
 * Entry points
 *   indexProduct(sp)          — index / overwrite a single catalog entry  (async, no-wait)
 *   bulkIndex(list)           — bulk-index all entries (used by seeder at boot)
 *   removeFromIndex(id)       — delete a catalog entry (admin discontinue flow)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StandardProductIndexer {

    public static final String INDEX = ElasticsearchIndexInitializer.CATALOG_INDEX;

    private final ElasticsearchClient esClient;
    private final ElasticsearchIndexInitializer indexInitializer;

    // ── Single product (fire-and-forget) ─────────────────────────────────────

    @Async
    public void indexProduct(StandardProduct sp) {
        try {
            indexInitializer.createCatalogIndex();
            StandardCatalogDocument doc = toDocument(sp);
            esClient.index(i -> i
                    .index(INDEX)
                    .id(sp.getId().toString())
                    .document(doc));
            log.info("Indexed catalog entry id={} name={}", sp.getId(), sp.getName());
        } catch (Exception e) {
            log.error("Failed to index catalog entry id={}: {}", sp.getId(), e.getMessage());
        }
    }

    // ── Bulk index (used by seeder) ───────────────────────────────────────────

    public void bulkIndex(List<StandardProduct> products) {
        if (products == null || products.isEmpty()) return;

        indexInitializer.createCatalogIndex();

        List<BulkOperation> ops = new ArrayList<>();
        for (StandardProduct sp : products) {
            StandardCatalogDocument doc = toDocument(sp);
            ops.add(BulkOperation.of(b -> b.index(
                    IndexOperation.of(i -> i
                            .index(INDEX)
                            .id(sp.getId().toString())
                            .document(doc)))));
        }

        try {
            BulkResponse resp = esClient.bulk(BulkRequest.of(b -> b.operations(ops)));
            if (resp.errors()) {
                resp.items().stream()
                        .filter(item -> item.error() != null && item.error().reason() != null)
                        .forEach(item -> log.warn("ES bulk catalog error id={}: {}",
                                item.id(), item.error().reason()));
            }
            log.info("Bulk-indexed {} catalog entries to {}", ops.size(), INDEX);
        } catch (Exception e) {
            log.error("Catalog bulk index failed: {}", e.getMessage());
        }
    }

    // ── Remove (admin discontinue / delete) ──────────────────────────────────

    @Async
    public void removeFromIndex(UUID id) {
        try {
            esClient.delete(d -> d.index(INDEX).id(id.toString()));
            log.info("Removed catalog entry id={} from ES", id);
        } catch (Exception e) {
            log.warn("Could not remove catalog entry id={} from ES: {}", id, e.getMessage());
        }
    }

    // ── Document builder ──────────────────────────────────────────────────────

    private StandardCatalogDocument toDocument(StandardProduct sp) {
        return StandardCatalogDocument.builder()
                .catalogId(sp.getId().toString())
                .name(sp.getName())
                .description(sp.getDescription())
                .searchKeywords(sp.getSearchKeywords())
                .ean(sp.getEan())
                .productCode(sp.getProductCode())
                .brandId(sp.getBrandEntity() != null
                        ? sp.getBrandEntity().getId().toString() : null)
                .brandName(sp.getBrandEntity() != null
                        ? sp.getBrandEntity().getName() : null)
                .categoryId(sp.getCategory() != null
                        ? sp.getCategory().getId().toString() : null)
                .categoryName(sp.getCategory() != null
                        ? sp.getCategory().getName() : null)
                .primaryImageUrl(sp.getPrimaryImageUrl())
                .specifications(sp.getSpecifications())
                .isVerified(Boolean.TRUE.equals(sp.getIsVerified()))
                .status(sp.getStatus() != null ? sp.getStatus().name() : null)
                .createdAt(sp.getCreatedAt() != null
                        ? sp.getCreatedAt().toInstant() : java.time.Instant.now())
                .build();
    }
}
