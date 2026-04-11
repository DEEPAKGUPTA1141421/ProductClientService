package com.ProductClientService.ProductClientService.Service.admin;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.filter.*;
import com.ProductClientService.ProductClientService.Model.*;
import com.ProductClientService.ProductClientService.Repository.*;
import com.ProductClientService.ProductClientService.Service.CategoryFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminFilterService {

    private final FilterRepository filterRepository;
    private final FilterOptionRepository filterOptionRepository;
    private final CategoryFilterMappingRepository mappingRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryFilterService categoryFilterService;

    // ── Filter CRUD ───────────────────────────────────────────────────────────

    public ApiResponse<Object> getAllFilters() {
        List<FilterAdminResponseDto> result = filterRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(FilterAdminResponseDto::from)
                .toList();
        return new ApiResponse<>(true, "Filters fetched", result, 200);
    }

    public ApiResponse<Object> getFilter(UUID filterId) {
        Filter f = findFilterOrThrow(filterId);
        return new ApiResponse<>(true, "Filter fetched", FilterAdminResponseDto.from(f), 200);
    }

    @Transactional
    public ApiResponse<Object> createFilter(CreateFilterRequest req) {
        if (filterRepository.existsByFilterKey(req.getFilterKey())) {
            return new ApiResponse<>(false, "Filter key already exists: " + req.getFilterKey(), null, 409);
        }

        Filter f = new Filter();
        f.setFilterKey(req.getFilterKey());
        f.setLabel(req.getLabel());
        f.setFilterType(req.getFilterType());
        f.setParamKey(req.getParamKey());
        f.setAttributeName(req.getAttributeName());
        f.setMinValue(req.getMinValue());
        f.setMaxValue(req.getMaxValue());
        f.setIsGlobal(req.getIsGlobal() != null && req.getIsGlobal());
        f.setIsSystem(false); // only FilterDataInitializer sets isSystem
        f.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);

        filterRepository.save(f);
        return new ApiResponse<>(true, "Filter created", FilterAdminResponseDto.from(f), 201);
    }

    @Transactional
    public ApiResponse<Object> updateFilter(UUID filterId, UpdateFilterRequest req) {
        Filter f = findFilterOrThrow(filterId);

        if (req.getLabel() != null)        f.setLabel(req.getLabel());
        if (req.getFilterType() != null)   f.setFilterType(req.getFilterType());
        if (req.getParamKey() != null)     f.setParamKey(req.getParamKey());
        if (req.getAttributeName() != null) f.setAttributeName(req.getAttributeName());
        if (req.getMinValue() != null)     f.setMinValue(req.getMinValue());
        if (req.getMaxValue() != null)     f.setMaxValue(req.getMaxValue());
        if (req.getIsGlobal() != null)     f.setIsGlobal(req.getIsGlobal());
        if (req.getDisplayOrder() != null) f.setDisplayOrder(req.getDisplayOrder());

        filterRepository.save(f);

        // Evict all category caches that might use this filter
        evictAllCategoryFilterCaches();

        return new ApiResponse<>(true, "Filter updated", FilterAdminResponseDto.from(f), 200);
    }

    @Transactional
    public ApiResponse<Object> deleteFilter(UUID filterId) {
        Filter f = findFilterOrThrow(filterId);

        if (Boolean.TRUE.equals(f.getIsSystem())) {
            return new ApiResponse<>(false, "System filters cannot be deleted", null, 403);
        }

        filterRepository.delete(f);
        evictAllCategoryFilterCaches();
        return new ApiResponse<>(true, "Filter deleted", null, 200);
    }

    // ── Filter Option CRUD ────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> addOption(UUID filterId, CreateFilterOptionRequest req) {
        Filter f = findFilterOrThrow(filterId);

        if (filterOptionRepository.existsByFilterIdAndOptionKey(filterId, req.getOptionKey())) {
            return new ApiResponse<>(false, "Option key already exists: " + req.getOptionKey(), null, 409);
        }

        FilterOption opt = new FilterOption();
        opt.setFilter(f);
        opt.setOptionKey(req.getOptionKey());
        opt.setLabel(req.getLabel());
        opt.setValue(req.getValue());
        opt.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);

        filterOptionRepository.save(opt);
        evictAllCategoryFilterCaches();

        return new ApiResponse<>(true, "Option added", FilterAdminResponseDto.OptionDto.from(opt), 201);
    }

    @Transactional
    public ApiResponse<Object> updateOption(UUID optionId, UpdateFilterOptionRequest req) {
        FilterOption opt = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found: " + optionId));

        if (req.getLabel() != null)        opt.setLabel(req.getLabel());
        if (req.getValue() != null)        opt.setValue(req.getValue());
        if (req.getDisplayOrder() != null) opt.setDisplayOrder(req.getDisplayOrder());

        filterOptionRepository.save(opt);
        evictAllCategoryFilterCaches();

        return new ApiResponse<>(true, "Option updated", FilterAdminResponseDto.OptionDto.from(opt), 200);
    }

    @Transactional
    public ApiResponse<Object> deleteOption(UUID optionId) {
        FilterOption opt = filterOptionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found: " + optionId));

        filterOptionRepository.delete(opt);
        evictAllCategoryFilterCaches();

        return new ApiResponse<>(true, "Option deleted", null, 200);
    }

    // ── Category ↔ Filter assignment ──────────────────────────────────────────

    public ApiResponse<Object> getCategoryFilters(UUID categoryId) {
        ensureCategoryExists(categoryId);
        List<FilterAdminResponseDto> result = mappingRepository
                .findByCategoryIdOrderByDisplayOrderAsc(categoryId)
                .stream()
                .map(m -> FilterAdminResponseDto.from(m.getFilter()))
                .toList();
        return new ApiResponse<>(true, "Category filters fetched", result, 200);
    }

    @Transactional
    public ApiResponse<Object> assignFilterToCategory(UUID categoryId, AssignFilterRequest req) {
        ensureCategoryExists(categoryId);
        Filter filter = findFilterOrThrow(req.getFilterId());

        if (Boolean.TRUE.equals(filter.getIsGlobal())) {
            return new ApiResponse<>(false,
                    "Global filters appear automatically — no assignment needed", null, 400);
        }

        if (mappingRepository.existsByCategoryIdAndFilterId(categoryId, req.getFilterId())) {
            return new ApiResponse<>(false, "Filter already assigned to this category", null, 409);
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        CategoryFilterMapping mapping = new CategoryFilterMapping();
        mapping.setCategory(category);
        mapping.setFilter(filter);
        mapping.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        mapping.setIsActive(true);

        mappingRepository.save(mapping);
        categoryFilterService.evictFilterCache(categoryId);

        return new ApiResponse<>(true, "Filter assigned to category", null, 201);
    }

    @Transactional
    public ApiResponse<Object> unassignFilterFromCategory(UUID categoryId, UUID filterId) {
        ensureCategoryExists(categoryId);

        if (!mappingRepository.existsByCategoryIdAndFilterId(categoryId, filterId)) {
            return new ApiResponse<>(false, "Mapping not found", null, 404);
        }

        mappingRepository.deleteByCategoryIdAndFilterId(categoryId, filterId);
        categoryFilterService.evictFilterCache(categoryId);

        return new ApiResponse<>(true, "Filter unassigned from category", null, 200);
    }

    @Transactional
    public ApiResponse<Object> toggleCategoryFilter(UUID categoryId, UUID filterId, boolean active) {
        ensureCategoryExists(categoryId);

        CategoryFilterMapping mapping = mappingRepository
                .findByCategoryIdAndFilterId(categoryId, filterId)
                .orElseThrow(() -> new RuntimeException("Mapping not found"));

        mapping.setIsActive(active);
        mappingRepository.save(mapping);
        categoryFilterService.evictFilterCache(categoryId);

        return new ApiResponse<>(true, "Filter " + (active ? "enabled" : "disabled"), null, 200);
    }

    @Transactional
    public ApiResponse<Object> updateDisplayOrder(UUID categoryId, UUID filterId, int displayOrder) {
        ensureCategoryExists(categoryId);

        CategoryFilterMapping mapping = mappingRepository
                .findByCategoryIdAndFilterId(categoryId, filterId)
                .orElseThrow(() -> new RuntimeException("Mapping not found"));

        mapping.setDisplayOrder(displayOrder);
        mappingRepository.save(mapping);
        categoryFilterService.evictFilterCache(categoryId);

        return new ApiResponse<>(true, "Display order updated", null, 200);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Filter findFilterOrThrow(UUID filterId) {
        return filterRepository.findById(filterId)
                .orElseThrow(() -> new RuntimeException("Filter not found: " + filterId));
    }

    private void ensureCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException("Category not found: " + categoryId);
        }
    }

    /**
     * Called after any mutation that could affect rendered filters.
     * Evicts all cached category filter responses so the next request rebuilds
     * from the updated DB state.
     */
    private void evictAllCategoryFilterCaches() {
        categoryFilterService.evictAllFilterCaches();
    }
}
