package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.UUID;

public record ProductVariantResponseDto(
        UUID id,
        String sku,
        String price,
        int stock) {
}