package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.UUID;

public record CatalogDraftBrandKeywordsDto(
        UUID brandId,
        String searchKeywords
) {}
