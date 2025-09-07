package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.UUID;

public record CategoryAttributeRequest(UUID categoryId,
        UUID attributeId,
        Boolean isRequired,
        Boolean isImageAttribute,
        Boolean isVariantAttribute) {
}
