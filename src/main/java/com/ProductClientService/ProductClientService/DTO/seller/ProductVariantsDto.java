package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductVariantsDto(
        UUID productId,
        List<VariantItem> variants) {

    public record VariantItem(
            Map<String, String> combination,
            String label,
            double price,
            double mrp,
            int stock,
            String sku) {
    }
}
