package com.ProductClientService.ProductClientService.DTO.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated shop listing response.
 * Matches Spring Page<T> JSON shape so the Flutter provider
 * can parse body['data']['content'] and body['data']['last'].
 */
@Data
@Builder
public class ShopPageResponse {
    private List<ShopSummaryDto> content;
    private int page;
    private int pageSize;
    private long totalElements;
    private boolean last;
}
