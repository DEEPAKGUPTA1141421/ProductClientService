package com.ProductClientService.ProductClientService.DTO.similarity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact item payload. Payload budget: ≤10KB for a list of 20 → ~400
 * bytes per item max. Description, tags, attributes are intentionally
 * omitted; the client hydrates the full product via /api/v1/product/{id}
 * on tap.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimilarProductDto {
    private String productId;
    private String title;
    private Long pricePaise;
    private Integer discountPct;
    private String thumbnailUrl;
    private Double avgRating;
    private Double score;
}
