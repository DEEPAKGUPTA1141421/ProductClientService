package com.ProductClientService.ProductClientService.DTO;

import lombok.*;

import java.util.*;

import com.ProductClientService.ProductClientService.Model.Section.SectionType;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionRequest {
    private String title;
    private SectionType type;
    private JsonNode config; // json string
    private Integer position;
    private Boolean active;
    private String category;
    private List<SectionItemRequest> items;
}

// hhiuhiuhuhi hjkhh huihuihbuhhhhhuui