package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ProductTagRequestDto(

                @NotNull(message = "Product ID must not be null") UUID product_id,

                @NotNull(message = "Tags list must not be null") @NotEmpty(message = "Tags list must not be empty") @Size(max = 20, message = "You can add at most 20 tags") List<@NotBlank(message = "Tag must not be blank") @Size(max = 50, message = "Tag must not exceed 50 characters") @Pattern(regexp = "^[a-zA-Z0-9-_ ]+$", message = "Tag contains invalid characters") String> tags) {
}
