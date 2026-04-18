package com.ProductClientService.ProductClientService.DTO.admin;

import java.util.UUID;

public interface ProductAttributeForIntentProjection {
    UUID getProductId();
    UUID getCategoryId();
    String getCategoryName();
    UUID getBrandId();
    String getBrandName();
    String getAttributeName();
    String getAttributeValue();
    Boolean getIsVariantAttribute();
    Boolean getIsImageAttribute();
}
