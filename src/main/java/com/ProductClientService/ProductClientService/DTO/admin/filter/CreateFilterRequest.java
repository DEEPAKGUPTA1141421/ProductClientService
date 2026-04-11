package com.ProductClientService.ProductClientService.DTO.admin.filter;

import com.ProductClientService.ProductClientService.Model.Filter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFilterRequest {

    @NotBlank
    private String filterKey;       // e.g. "storage", "color"

    @NotBlank
    private String label;           // e.g. "Storage", "Color"

    @NotNull
    private Filter.FilterType filterType;  // SINGLE_SELECT | MULTI_SELECT | RANGE

    @NotBlank
    private String paramKey;        // e.g. "attributeValues", "brandIds"

    private String attributeName;   // required when paramKey = "attributeValues"

    private Double minValue;        // for RANGE filters
    private Double maxValue;        // for RANGE filters

    private Boolean isGlobal = false;

    private Integer displayOrder = 0;
}
