package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

public record ProductVariantsDto(
        UUID productId,
        List<String> stock,
        List<String> skus,
        String step,
        List<String> price) {
}
// kjjj