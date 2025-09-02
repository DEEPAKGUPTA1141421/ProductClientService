package com.ProductClientService.ProductClientService.DTO;

import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.SectionItem.ItemType;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionItemRequest {
    private ItemType itemType;
    private String itemRefId;
    private JsonNode metadata;
}