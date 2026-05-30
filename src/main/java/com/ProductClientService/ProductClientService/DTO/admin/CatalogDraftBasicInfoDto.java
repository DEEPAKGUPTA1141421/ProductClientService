package com.ProductClientService.ProductClientService.DTO.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CatalogDraftBasicInfoDto(

        @NotBlank(message = "name is required")
        String name,

        String description,

        @NotNull(message = "categoryId is required")
        UUID categoryId,

        String ean,

        String productCode
) {}
