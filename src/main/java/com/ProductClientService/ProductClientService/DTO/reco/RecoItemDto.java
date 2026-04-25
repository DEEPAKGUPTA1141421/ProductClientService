package com.ProductClientService.ProductClientService.DTO.reco;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Compact — ≤10KB for 20 items on 2G/3G. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoItemDto {
    private String productId;
    private String title;
    private Long pricePaise;
    private Integer discountPct;
    private String thumbnailUrl;
    private Double avgRating;
    private Double score;
    /** e.g. "based_on_recent_views", "popular_in_your_city", "trending_cod". */
    private String reason;
}
