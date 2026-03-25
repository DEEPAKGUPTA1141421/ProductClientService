package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.UUID;

public record ProductAttributeForIntentProjection(
        UUID productId,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String attributeName,
        String attributeValue,
        Boolean isVariantAttribute,
        Boolean isImageAttribute) {
}
