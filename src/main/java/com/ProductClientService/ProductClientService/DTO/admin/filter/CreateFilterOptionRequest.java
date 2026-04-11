package com.ProductClientService.ProductClientService.DTO.admin.filter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFilterOptionRequest {

    @NotBlank
    private String optionKey;   // stable key, e.g. "128_gb", "price_asc"

    @NotBlank
    private String label;       // display text, e.g. "128 GB"

    @NotBlank
    private String value;       // API value sent in request, e.g. "128 GB"

    private Integer displayOrder = 0;
}
