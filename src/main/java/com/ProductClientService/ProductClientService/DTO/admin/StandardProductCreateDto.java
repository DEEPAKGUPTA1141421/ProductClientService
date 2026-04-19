package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.UUID;

public record StandardProductCreateDto(
        String name,
        String description,
        UUID categoryId,
        UUID brandId,
        String ean,
        String productCode,
        String specifications,
        String primaryImageUrl,
        String searchKeywords) {
}
