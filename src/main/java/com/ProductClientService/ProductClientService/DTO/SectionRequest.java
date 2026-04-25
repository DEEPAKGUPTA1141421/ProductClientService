package com.ProductClientService.ProductClientService.DTO;

import lombok.*;

import java.util.*;

import com.ProductClientService.ProductClientService.Model.Section.SectionType;
import com.ProductClientService.ProductClientService.Model.Section.DataSource;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionRequest {
    private String title;
    private SectionType type;
    private String widgetKey;           // e.g. "product_grid_v1"
    private DataSource dataSource;      // STATIC, RECO_FOR_YOU, etc.
    private JsonNode config;            // layout config
    private JsonNode dataParams;        // data source params
    private Integer position;
    private Boolean active;
    private String category;
    private OffsetDateTime startsAt;    // campaign start time
    private OffsetDateTime endsAt;      // campaign end time
    private JsonNode audience;          // targeting criteria
    private List<SectionItemRequest> items;
}

// hhiuhiuhuhi hjkhh huihuihbuhhhhhuui