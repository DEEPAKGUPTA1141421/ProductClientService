package com.ProductClientService.ProductClientService.DTO.sections;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionResponseDto {

    private String id;        // Section UUID
    private String title;     // e.g. "Suggested for You"
    private String widgetKey; // e.g. "product_grid_v1", "banner_hero_v1"
    private String dataKind;  // derived from section.type.name() (e.g. "PRODUCT_GRID", "BANNER")
    private int position;     // sort order on page

    // Layout config (columns, rows, cardVariant, theme, etc.)
    private JsonNode config;

    // Hydrated items (embedded for single-page load)
    private List<SectionItemResponseDto> items;

    // Pagination metadata (for infinite-scroll sections; null for others)
    private PaginationMeta pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMeta {
        private boolean hasMore;
        private String nextCursor; // opaque token for the next page
    }
}
