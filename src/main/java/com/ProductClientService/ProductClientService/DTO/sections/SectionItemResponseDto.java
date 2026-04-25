package com.ProductClientService.ProductClientService.DTO.sections;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionItemResponseDto {

    private String itemType;  // PRODUCT, BANNER, BRAND, CATEGORY, etc.
    private String itemRefId; // Product UUID, Banner ID, Brand ID, etc.

    // Hydrated product fields (null for non-PRODUCT items)
    private String productId;
    private String title;
    private String thumbnailUrl;
    private Long pricePaise;
    private Integer discountPct;
    private Double avgRating;
    private Double score; // for ranking in reco contexts

    // Raw metadata for BANNER/BRAND/CATEGORY items (image URL, deeplinks, etc.)
    private JsonNode metadata;
}
