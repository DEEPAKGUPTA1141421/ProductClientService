package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

public record ProductAttributeResponseDto(
                UUID id,
                String name,
                String value,
                List<ProductVariantResponseDto> variants) {
}
