package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.filter.CategoryFiltersResponse;
import com.ProductClientService.ProductClientService.DTO.filter.FilterDto;
import com.ProductClientService.ProductClientService.DTO.filter.FilterOptionDto;
import com.ProductClientService.ProductClientService.Model.CategoryFilterMapping;
import com.ProductClientService.ProductClientService.Model.Filter;
import com.ProductClientService.ProductClientService.Model.FilterOption;
import com.ProductClientService.ProductClientService.Repository.CategoryFilterMappingRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.FilterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * CategoryFilterService
 * ─────────────────────
 * Builds the ordered filter list for a category entirely from DB tables.
 * No values are hardcoded here — Sort, Price, Rating, and all custom
 * attribute filters are stored in the `filters` / `filter_options` /
 * `category_filter_mapping` tables and managed via AdminFilterController.
 *
 * Response order
 * ──────────────
 * 1. Global filters (is_global = true) — ordered by display_order.
 * These always appear without any category mapping (Sort, Price, Rating).
 * 2. Category-specific filters — linked via category_filter_mapping,
 * ordered by mapping.display_order.
 * If the category has no mappings, the service walks up the parent chain.
 *
 * Caching
 * ───────
 * Key : "filters:category:{categoryId}"
 * TTL : 15 minutes.
 * Evict: AdminFilterService calls evictFilterCache(id) or
 * evictAllFilterCaches()
 * after any mutation so the next request rebuilds from DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryFilterService {

    private final FilterRepository filterRepository;
    private final CategoryFilterMappingRepository mappingRepository;
    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "filters:category:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    // ── Public read API ───────────────────────────────────────────────────────

    public CategoryFiltersResponse getFiltersForCategory(UUID categoryId) {

        String cacheKey = CACHE_PREFIX + categoryId;

        // 1. Redis cache
        // String cached = redis.opsForValue().get(cacheKey);
        // if (cached != null) {
        // try {
        // log.debug("Filter cache HIT: {}", cacheKey);
        // return objectMapper.readValue(cached, CategoryFiltersResponse.class);
        // } catch (Exception e) {
        // log.warn("Filter cache deserialisation failed for {}: {}", cacheKey,
        // e.getMessage());
        // }
        // }

        // 2. Load category name
        String categoryName = categoryRepository.findById(categoryId)
                .map(c -> c.getName())
                .orElse("Unknown");

        // 3. Global filters — always shown, no mapping needed
        List<Filter> globalFilters = filterRepository.findByIsGlobalTrueOrderByDisplayOrderAsc();

        // 4. Category-specific filters (with hierarchy walk)
        HierarchyResult result = findCategoryFiltersWithHierarchy(categoryId);

        // 5. Build response: globals first, then category-specific
        List<FilterDto> filters = new ArrayList<>();
        globalFilters.stream().map(this::toFilterDto).forEach(filters::add);
        result.mappings().stream()
                .map(m -> toFilterDto(m.getFilter()))
                .forEach(filters::add);

        CategoryFiltersResponse response = CategoryFiltersResponse.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .inheritedFromParent(result.inherited())
                .filters(filters)
                .build();

        // 6. Cache
        // try {
        // redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response),
        // CACHE_TTL);
        // } catch (Exception e) {
        // log.warn("Failed to cache filters for {}: {}", cacheKey, e.getMessage());
        // }

        return response;
    }

    // ── Cache eviction ────────────────────────────────────────────────────────

    /** Evicts the cached response for a single category. */
    public void evictFilterCache(UUID categoryId) {
        redis.delete(CACHE_PREFIX + categoryId);
        log.info("Evicted filter cache for categoryId={}", categoryId);
    }

    /**
     * Evicts ALL category filter caches.
     * Called by AdminFilterService when a global filter or filter option changes,
     * since the change affects every category's response.
     */
    public void evictAllFilterCaches() {
        Set<String> keys = redis.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("Evicted {} category filter cache entries", keys.size());
        }
    }

    // ── Hierarchy walk ────────────────────────────────────────────────────────

    /**
     * Tries the given categoryId first. If it has no active mappings,
     * walks up the parent chain (max 4 hops) until mappings are found.
     */
    private HierarchyResult findCategoryFiltersWithHierarchy(UUID categoryId) {
        UUID currentId = categoryId;
        boolean inherited = false;

        for (int depth = 0; depth < 4 && currentId != null; depth++) {
            List<CategoryFilterMapping> mappings = mappingRepository
                    .findByCategoryIdAndIsActiveTrueOrderByDisplayOrderAsc(currentId);

            if (!mappings.isEmpty()) {
                return new HierarchyResult(mappings, inherited);
            }

            UUID parentId = categoryRepository.findParentIdById(currentId).orElse(null);
            if (parentId == null)
                break;
            currentId = parentId;
            inherited = true;
        }

        return new HierarchyResult(List.of(), false);
    }

    private record HierarchyResult(List<CategoryFilterMapping> mappings, boolean inherited) {
    }

    // ── Entity → DTO mapping ──────────────────────────────────────────────────

    private FilterDto toFilterDto(Filter f) {
        List<FilterOptionDto> options = f.getOptions().stream()
                .map(this::toOptionDto)
                .toList();

        return FilterDto.builder()
                .id(f.getFilterKey()) // use filterKey as the stable client-side id
                .label(f.getLabel())
                .filterType(FilterDto.FilterType.valueOf(f.getFilterType().name()))
                .paramKey(f.getParamKey())
                .attributeName(f.getAttributeName())
                .options(options)
                .minValue(f.getMinValue())
                .maxValue(f.getMaxValue())
                .build();
    }

    private FilterOptionDto toOptionDto(FilterOption o) {
        return FilterOptionDto.builder()
                .id(o.getOptionKey())
                .label(o.getLabel())
                .value(o.getValue())
                .build();
    }
}
// juikiy77yiyuiuy