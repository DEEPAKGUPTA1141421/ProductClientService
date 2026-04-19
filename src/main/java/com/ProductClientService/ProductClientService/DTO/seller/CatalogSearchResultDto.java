package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.UUID;

public record CatalogSearchResultDto(
        UUID id,
        String name,
        String description,
        String primaryImageUrl,
        String brandName,
        String categoryName,
        String specifications,
        String ean,
        String productCode) {
}
