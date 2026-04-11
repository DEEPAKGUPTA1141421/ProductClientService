package com.ProductClientService.ProductClientService.DTO.admin.filter;

import lombok.Data;

@Data
public class UpdateFilterOptionRequest {

    private String label;
    private String value;
    private Integer displayOrder;
}
