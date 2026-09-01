package com.ProductClientService.ProductClientService.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requested by the app BEFORE it uploads a product photo/video directly to
 * Cloudinary — the response carries a short-lived, server-signed payload
 * scoped to exactly this product + purpose, so the client never has the
 * Cloudinary API secret and can't redirect the upload elsewhere.
 */
public record MediaSignatureRequestDto(
        @NotBlank @Pattern(regexp = "cover|attribute", message = "purpose must be 'cover' or 'attribute'") String purpose,

        // Required when purpose == "attribute": "{categoryAttributeId}::{value}"
        String attributeKey,

        @NotBlank @Pattern(regexp = "image|video", message = "resourceType must be 'image' or 'video'") String resourceType) {
}
