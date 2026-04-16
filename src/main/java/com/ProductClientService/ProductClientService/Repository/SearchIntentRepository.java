package com.ProductClientService.ProductClientService.Repository;

/**
 * SearchIntentRepository is no longer active.
 * Search intents are stored in the "search-intents-v1" Elasticsearch index,
 * accessed via ElasticsearchClient in SearchIntentService and
 * SearchIntentGeneratorService.
 *
 * This file is retained as a placeholder. It can be safely deleted.
 *
 * @deprecated No-op — do not inject or use.
 */
@Deprecated
public interface SearchIntentRepository {
    // intentionally empty — no JpaRepository extension, no Spring Data bean
}
