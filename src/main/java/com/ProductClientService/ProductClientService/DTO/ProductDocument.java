package com.ProductClientService.ProductClientService.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDocument {
    private String id; // UUID
    private String name;
    private String description;
    private String sellerId;
    private String sellerName;
    private String categoryId;
    private String categoryName;
    private String brandId;
    private String brandName;
    private String createdAt;
}
