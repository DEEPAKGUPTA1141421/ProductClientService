package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Sent AFTER the client has uploaded directly to Cloudinary, to attach the
 * result to the product. Carries only metadata (URL/public_id/byte count) —
 * never the file itself — so this call is small regardless of file size.
 * The backend re-validates {@code bytes} against the configured limit and
 * that {@code publicId} actually belongs to this product before trusting
 * any of it.
 */
public record MediaConfirmRequestDto(
        @NotNull UUID productId,

        @NotBlank @Pattern(regexp = "cover|attribute", message = "purpose must be 'cover' or 'attribute'") String purpose,

        // Required when purpose == "attribute": "{categoryAttributeId}::{value}"
        String attributeKey,

        @NotBlank String publicId,
        @NotBlank String secureUrl,
        @NotBlank @Pattern(regexp = "image|video", message = "resourceType must be 'image' or 'video'") String resourceType,
        @PositiveOrZero long bytes) {
}
