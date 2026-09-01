package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductVariantsDto(
        @NotNull(message = "productId is required") UUID productId,

        @NotEmpty(message = "variants list cannot be empty") @Valid List<VariantItem> variants) {

    public record VariantItem(
            @NotEmpty(message = "combination cannot be empty") Map<String, @NotBlank String> combination,

            @NotBlank(message = "label is required") String label,

            @Positive(message = "price must be greater than 0") double price,

            @Positive(message = "mrp must be greater than 0") double mrp,

            @PositiveOrZero(message = "stock cannot be negative") int stock,

            @NotBlank(message = "sku is required") String sku,

            // Optional — configure a discount at variant-creation time.
            @Valid VariantDiscountDto discount) {
    }
}
