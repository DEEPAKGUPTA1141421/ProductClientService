package com.ProductClientService.ProductClientService.DTO.seller;

/**
 * Everything the app needs to POST a file straight to Cloudinary's upload
 * API without ever touching our backend's bandwidth. {@code publicId} and
 * {@code folder} are bound into the signature, so the client cannot alter
 * where the asset lands without invalidating it. {@code maxBytes} lets the
 * client reject an oversized file before it even starts uploading — the
 * backend independently re-checks the real size at confirm time.
 */
public record MediaSignatureResponseDto(
        String cloudName,
        String apiKey,
        String signature,
        long timestamp,
        String folder,
        String publicId,
        String resourceType,
        long maxBytes) {
}
