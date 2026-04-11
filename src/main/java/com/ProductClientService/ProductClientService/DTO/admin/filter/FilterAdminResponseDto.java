package com.ProductClientService.ProductClientService.DTO.admin.filter;

import com.ProductClientService.ProductClientService.Model.Filter;
import com.ProductClientService.ProductClientService.Model.FilterOption;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Full filter representation returned by admin endpoints. */
@Data
@Builder
public class FilterAdminResponseDto {

    private UUID id;
    private String filterKey;
    private String label;
    private Filter.FilterType filterType;
    private String paramKey;
    private String attributeName;
    private Double minValue;
    private Double maxValue;
    private Boolean isSystem;
    private Boolean isGlobal;
    private Integer displayOrder;
    private List<OptionDto> options;

    @Data
    @Builder
    public static class OptionDto {
        private UUID id;
        private String optionKey;
        private String label;
        private String value;
        private Integer displayOrder;

        public static OptionDto from(FilterOption o) {
            return OptionDto.builder()
                    .id(o.getId())
                    .optionKey(o.getOptionKey())
                    .label(o.getLabel())
                    .value(o.getValue())
                    .displayOrder(o.getDisplayOrder())
                    .build();
        }
    }

    public static FilterAdminResponseDto from(Filter f) {
        return FilterAdminResponseDto.builder()
                .id(f.getId())
                .filterKey(f.getFilterKey())
                .label(f.getLabel())
                .filterType(f.getFilterType())
                .paramKey(f.getParamKey())
                .attributeName(f.getAttributeName())
                .minValue(f.getMinValue())
                .maxValue(f.getMaxValue())
                .isSystem(f.getIsSystem())
                .isGlobal(f.getIsGlobal())
                .displayOrder(f.getDisplayOrder())
                .options(f.getOptions().stream()
                        .map(OptionDto::from)
                        .toList())
                .build();
    }
}
