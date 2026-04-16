package com.ProductClientService.ProductClientService.Model;

/**
 * SearchIntent is no longer a JPA entity.
 * Search intents are stored exclusively in the "search-intents-v1" Elasticsearch index.
 * See SearchIntentDocument for the ES representation.
 * This class is retained only to avoid breaking any existing import references
 * during the migration; it can be safely deleted once all usages are removed.
 *
 * @deprecated Use SearchIntentDocument (ES) instead.
 */
@Deprecated
public class SearchIntent {
    // intentionally empty — no JPA, no table
}
