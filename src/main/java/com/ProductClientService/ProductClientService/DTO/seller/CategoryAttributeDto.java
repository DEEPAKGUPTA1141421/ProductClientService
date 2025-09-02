package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;

public record CategoryAttributeDto(
    UUID id,
    UUID categoryId,
    List<AttributeDto> attributeIds
) {}

