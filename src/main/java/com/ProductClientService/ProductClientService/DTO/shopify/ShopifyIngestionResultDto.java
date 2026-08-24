package com.ProductClientService.ProductClientService.DTO.shopify;

import java.util.List;

public record ShopifyIngestionResultDto(
        int totalFetched,
        int inserted,
        int updated,
        int skipped,
        int failed,
        List<String> errors
) {}
