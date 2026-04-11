package com.ProductClientService.ProductClientService.DTO.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionDto {
    private String id;      // Unique key (e.g. "128gb", "price_asc")
    private String label;   // Display text shown in UI (e.g. "128 GB", "Price: Low to High")
    private String value;   // API value sent in request param (e.g. "128 GB", "price_asc")
}
