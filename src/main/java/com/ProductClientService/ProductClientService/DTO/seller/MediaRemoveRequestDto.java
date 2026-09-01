package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Removes one already-uploaded media item: deletes the DB row/reference AND
 * destroys the matching Cloudinary asset. {@code url} identifies which item
 * (a product can have several attribute images sharing the same product +
 * attributeKey), so it must match exactly what was returned at confirm time.
 */
public record MediaRemoveRequestDto(
        @NotNull UUID productId,

        @NotBlank @Pattern(regexp = "cover|attribute", message = "purpose must be 'cover' or 'attribute'") String purpose,

        // Required when purpose == "attribute": "{categoryAttributeId}::{value}"
        String attributeKey,

        @NotBlank String url) {
}
