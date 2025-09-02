package com.ProductClientService.ProductClientService.DTO.seller;

import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.Product.Step;

public record ProductAttributeDto(
                UUID productId,
                List<UUID> attributeId,
                String step,
                List<List<String>> values) {
}
