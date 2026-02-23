package com.ProductClientService.ProductClientService.DTO.seller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductAttributeDto(
        @JsonProperty("productId") @NotNull(message = "Product ID cannot be null") UUID productId,
        @JsonProperty("categoryAttributeId") @NotEmpty(message = "Category Attribute IDs cannot be empty") List<@NotNull(message = " Category Attribute ID cannot be null") UUID> categoryAttributeId,
        @JsonProperty("productAttributeIds") List<UUID> productAttributeIds,
        @JsonProperty("step") @NotNull(message = "Step cannot be null") String step,

        @JsonProperty("values") @NotEmpty(message = "Values list cannot be empty") List<@NotEmpty(message = "Each attribute must have values") List<@NotNull(message = "Value cannot be null") String>> values) {
}

// jou8 nmiuly7uiknjmiu hihiu huihyiuyiuinn,n,
// hui huiu huiuuuj jkjiu nkji jgyjh jhku bhkhjuk kjhhjk