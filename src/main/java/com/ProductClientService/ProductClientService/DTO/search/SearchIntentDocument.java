package com.ProductClientService.ProductClientService.DTO.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Elasticsearch document for the "search-intents-v1" index.
 * The keyword field uses the search_as_you_type mapping so ES auto-creates
 * the ._2gram, ._3gram, and ._index_prefix sub-fields used for autocomplete.
 *
 * filterPayload is stored as a plain Map so Jackson/JacksonJsonpMapper
 * serializes/deserializes it cleanly without JsonNode complexity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIntentDocument {
    private String keyword;
    private String imageUrl;
    private String suggestionType;
    private Map<String, Object> filterPayload;
    @Builder.Default
    private long searchCount = 0L;
    @Builder.Default
    private long clickCount = 0L;
}
