package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DraftProductFullDto(
        UUID productId,
        String currentStep,

        // Step 1 — basic info
        StepBasicInfo basicInfo,

        // Step 2 — attributes
        List<StepAttribute> attributes,

        // Step 3 — variants
        List<StepVariant> variants,

        // Step 4 — media
        StepMedia media,

        // Step 5 — tags
        List<StepTag> tags,

        // Step 6 — brand
        StepBrand brand) {

    public record StepBasicInfo(
            String name,
            String description,
            UUID categoryId,
            String categoryName) {
    }

    public record StepAttribute(
            UUID id,
            UUID categoryAttributeId,
            String name,
            String value,
            boolean isImageAttribute,
            boolean isVariantAttribute,
            List<String> images) {
    }

    public record StepVariant(
            UUID id,
            String sku,
            String label,
            double price,
            double mrp,
            int stock,
            Map<String, String> combination) {
    }

    public record StepMedia(
            String coverImageUrl,
            Map<String, List<String>> attributeMedia) {
    }

    public record StepTag(UUID id, String name) {
    }

    public record StepBrand(UUID id, String name) {
    }
}
