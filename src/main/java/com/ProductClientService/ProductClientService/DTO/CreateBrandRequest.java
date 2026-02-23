package com.ProductClientService.ProductClientService.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateBrandRequest {

    @NotBlank(message = "Brand name is required")
    private String name;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private String description;
    private String website;
}
