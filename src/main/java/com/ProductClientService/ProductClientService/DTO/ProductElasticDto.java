package com.ProductClientService.ProductClientService.DTO;

import java.time.ZonedDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductElasticDto {
    private UUID id;
    private String name;
    private String description;
    private UUID sellerId;
    private String sellerName;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private ZonedDateTime createdAt;

    // Constructor matching JPQL projection
    public ProductElasticDto(UUID id,
            String name,
            String description,
            UUID sellerId,
            String sellerName,
            UUID categoryId,
            String categoryName,
            UUID brandId,
            String brandName,
            ZonedDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.brandId = brandId;
        this.brandName = brandName;
        this.createdAt = createdAt;
    }
}

// gujguygjhgvhg njnjjnnnjnnj njnjnjnjnjjjnjnjjhjhjhhkkhb
// ikgbhbihnbgjhgjgbjhguygbhbhj njnjnjnjnkjn nkjnjknkjjnnjn