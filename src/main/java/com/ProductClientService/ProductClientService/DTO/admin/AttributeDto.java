package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.List;
import java.util.UUID;
import com.ProductClientService.ProductClientService.Model.Attribute.FEILDTYPE;

public record AttributeDto(
        UUID id,
        String name,
        FEILDTYPE fieldType,
        Boolean isRequired,
        List<String> options,
        Boolean isRadio,
        Boolean is_Required_from_category_attribute,
        Boolean isImageAttribute,
        Boolean isVariantAttribute,
        Boolean isAdditionalAttribute) {
}

// klkj jlk kj kjk kjkjij huju hjuju jju