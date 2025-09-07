package com.ProductClientService.ProductClientService.DTO.seller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ProductAttributeDto(

        @NotNull(message = "Product ID cannot be null") UUID productId,

        @NotEmpty(message = "Attribute IDs cannot be empty") List<@NotNull(message = "Attribute ID cannot be null") UUID> categoryAttributeId,

        @NotNull(message = "Step cannot be null") String step,

        @NotEmpty(message = "Values list cannot be empty") List<@NotEmpty(message = "Each attribute must have values") List<@NotNull(message = "Value cannot be null") String>> values) {
}
