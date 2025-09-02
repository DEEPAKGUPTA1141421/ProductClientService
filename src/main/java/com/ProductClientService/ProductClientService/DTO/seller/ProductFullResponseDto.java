package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

public record ProductFullResponseDto(
        UUID id,
        String name,
        String description,
        List<ProductAttributeResponseDto> attributes) {
}