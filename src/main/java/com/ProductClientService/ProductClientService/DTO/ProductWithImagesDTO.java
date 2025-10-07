package com.ProductClientService.ProductClientService.DTO;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductWithImagesDTO {
    private UUID id;
    private String name;
    private String description;
    private List<String> images;
}

// iuu8iu9ioi9oiyu8