package com.ProductClientService.ProductClientService.DTO.admin;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CatalogDraftSpecsDto(

        @NotNull(message = "specifications map is required")
        Map<String, String> specifications
) {}
