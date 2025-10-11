package com.ProductClientService.ProductClientService.DTO;

import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeDto {
    private UUID id;
    private String name;
    private String fieldType;
    private Boolean isRequired;
    private List<String> options;
}
