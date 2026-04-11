package com.ProductClientService.ProductClientService.DTO.admin.filter;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignFilterRequest {

    @NotNull
    private UUID filterId;

    private Integer displayOrder = 0;
}
