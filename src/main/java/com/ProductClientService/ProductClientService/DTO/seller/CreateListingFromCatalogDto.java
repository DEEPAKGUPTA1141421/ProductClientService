package com.ProductClientService.ProductClientService.DTO.seller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateListingFromCatalogDto(

        @NotNull(message = "standardProductId is required")
        UUID standardProductId,

        @NotNull(message = "variants list is required")
        @Size(min = 1, max = 20, message = "Provide between 1 and 20 variants")
        List<@Valid VariantItem> variants,

        // true → product goes LIVE immediately after listing is created
        boolean goLive

) {
    public record VariantItem(

            // e.g. {"Storage": "64GB", "Color": "Black"} — empty map for single-variant products
            Map<String, String> combination,

            // human-readable label derived from combination, e.g. "64GB Black"
            String label,

            @Positive(message = "price must be positive (in rupees)")
            double price,

            @PositiveOrZero(message = "mrp cannot be negative")
            double mrp,

            @Min(value = 0, message = "stock cannot be negative")
            @Max(value = 100000, message = "stock cannot exceed 100,000")
            int stock,

            // optional — auto-generated if blank
            String sku

    ) {}
}
