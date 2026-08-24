package com.ProductClientService.ProductClientService.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertConfigRequest {
    @NotBlank
    private String key;

    private String description;

    @NotNull
    private Object value;
}
