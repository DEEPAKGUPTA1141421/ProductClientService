package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.SearchIntent;
import com.ProductClientService.ProductClientService.Repository.SearchIntentRepository;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * SearchIntentService
 * ────────────────────
 * Handles the runtime side of search intents:
 *
 * 1. autocomplete(keyword)
 * Called as the user types. Returns matching SearchIntent rows
 * including their filterPayload so the frontend knows what filters
 * to pass to the product listing API when the user taps a suggestion.
 *
 * 2. recordClick(id)
 * Called when user taps a suggestion. Increments click count (async).
 * Frontend then calls the product search API with the filterPayload.
 *
 * 3. resolveFilter(id)
 * Returns the filterPayload JSON for a given SearchIntent ID.
 * This is the structured object the frontend sends to /products/search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentService {

    private final SearchIntentRepository searchIntentRepository;

    /**
     * Autocomplete endpoint — called while user types.
     *
     * Returns up to 10 suggestion objects. Each includes:
     * - id → used to call recordClick later
     * - keyword → display text in the suggestion list
     * - imageUrl → thumbnail shown beside the keyword
     * - filterPayload → the structured filter to use on tap
     */
    public ApiResponse<Object> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ApiResponse<>(false, "Keyword required", null, 400);
        }

        List<SearchIntent> results = keyword.length() < 3
                ? searchIntentRepository.findTopByKeywordStartingWith(keyword.trim())
                : searchIntentRepository.fuzzySearch(keyword.trim());

        // Increment search count for each returned intent (async, non-blocking)
        results.forEach(si -> incrementSearchCountAsync(si.getId()));

        List<SearchIntentDto> dtos = results.stream()
                .map(si -> new SearchIntentDto(
                        si.getId(),
                        si.getKeyword(),
                        si.getImageUrl(),
                        si.getSuggestionType(),
                        si.getFilterPayload()))
                .toList();

        return new ApiResponse<>(true, "Suggestions fetched", dtos, 200);
    }

    /**
     * Called when user taps a suggestion in the frontend.
     * Returns the filterPayload so the frontend can call the product search API.
     *
     * Flow:
     * frontend taps suggestion
     * → POST /search-intent/{id}/click
     * → receives { filterPayload: { categoryId, gender, color, ... } }
     * → calls GET /products/search?categoryId=...&gender=...&color=...
     */
    public ApiResponse<Object> recordClick(UUID intentId) {
        SearchIntent intent = searchIntentRepository.findById(intentId)
                .orElseThrow(() -> new RuntimeException("SearchIntent not found: " + intentId));

        incrementClickCountAsync(intentId);

        return new ApiResponse<>(true, "Filter payload resolved",
                new SearchIntentClickResponse(intent.getId(), intent.getKeyword(), intent.getFilterPayload()),
                200);
    }

    /**
     * Resolves just the filterPayload for a given intent.
     * Lightweight alternative to recordClick when you only need the filters.
     */
    public JsonNode resolveFilter(UUID intentId) {
        return searchIntentRepository.findById(intentId)
                .map(SearchIntent::getFilterPayload)
                .orElseThrow(() -> new RuntimeException("SearchIntent not found: " + intentId));
    }

    // ── Async analytics (fire-and-forget) ────────────────────────────────────────

    @Async
    public void incrementSearchCountAsync(UUID id) {
        try {
            searchIntentRepository.incrementSearchCount(id);
        } catch (Exception e) {
            log.warn("Failed to increment search count for {}", id);
        }
    }

    @Async
    public void incrementClickCountAsync(UUID id) {
        try {
            searchIntentRepository.incrementClickCount(id);
        } catch (Exception e) {
            log.warn("Failed to increment click count for {}", id);
        }
    }

    // ── DTOs (inner records for simplicity) ──────────────────────────────────────

    /**
     * Returned by autocomplete.
     *
     * The frontend uses filterPayload to:
     * 1. Display the suggestion with its thumbnail
     * 2. On tap → call POST /search-intent/{id}/click to confirm selection
     * and receive the definitive filterPayload to pass to product search
     */
    public record SearchIntentDto(
            UUID id,
            String keyword,
            String imageUrl,
            String suggestionType,
            JsonNode filterPayload // e.g. {"categoryId":"...", "gender":"men", "color":"red"}
    ) {
    }

    /**
     * Returned by recordClick.
     *
     * The frontend takes filterPayload and maps it to query params:
     * GET /api/v1/product/products/search
     * ?categoryId=<categoryId>
     * &gender=<gender> (passed as attributeName=gender&attributeValue=men)
     * &color=<color> (passed as attributeName=color&attributeValue=red)
     * &brandId=<brandId>
     * &size=<size>
     */
    public record SearchIntentClickResponse(
            UUID id,
            String keyword,
            JsonNode filterPayload) {
    }
}