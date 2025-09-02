package com.ProductClientService.ProductClientService.DTO.admin;

import jakarta.validation.constraints.NotBlank;

public record CategoryDto(
        @NotBlank(message = "Category  is required")
        String category,
        String parent
) {}


