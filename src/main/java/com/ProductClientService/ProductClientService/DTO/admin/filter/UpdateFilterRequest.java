package com.ProductClientService.ProductClientService.DTO.admin.filter;

import com.ProductClientService.ProductClientService.Model.Filter;
import lombok.Data;

@Data
public class UpdateFilterRequest {

    private String label;
    private Filter.FilterType filterType;
    private String paramKey;
    private String attributeName;
    private Double minValue;
    private Double maxValue;
    private Boolean isGlobal;
    private Integer displayOrder;
}
