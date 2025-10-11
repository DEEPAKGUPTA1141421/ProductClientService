package com.ProductClientService.ProductClientService.DTO;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailsDto {
    private String name;
    private String description;
    private String imageUrl;
}
