package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

public record ProductVariantsDto(
        UUID productId,
        List<UUID> productAttributeId,
        List<String> stock,
        List<String> skus,
        String step,
        List<String> price) {
    public ProductVariantsDto {
        if (productAttributeId == null || stock == null || skus == null || price == null) {
            throw new IllegalArgumentException("Lists cannot be null");
        }

        int size = productAttributeId
                .size();
        if (stock.size() != size || skus.size() != size || price.size() != size) {
            throw new IllegalArgumentException("All lists must have the same size");
        }
    }
}
// kjjj