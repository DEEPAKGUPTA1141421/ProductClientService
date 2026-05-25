package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.Map;
import java.util.UUID;

public record CatalogDraftResponseDto(
        UUID id,
        String draftStep,
        String status,

        // Step 1
        String name,
        String description,
        UUID categoryId,
        String categoryName,
        String ean,
        String productCode,

        // Step 2
        Map<String, String> specifications,

        // Step 3
        String primaryImageUrl,

        // Step 4
        UUID brandId,
        String brandName,
        String searchKeywords
) {}
