package com.ProductClientService.ProductClientService.DTO;

import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.SectionItem.ItemType;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionItemRequest {
    private ItemType itemType;
    private String itemRefId;
    private int position;    // sort order within section
    private JsonNode metadata;
}