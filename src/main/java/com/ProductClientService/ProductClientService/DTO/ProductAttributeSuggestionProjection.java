package com.ProductClientService.ProductClientService.DTO;

import java.util.UUID;

public interface ProductAttributeSuggestionProjection {

    UUID getProductId();

    String getCategoryName();

    String getAttributeName();

    String getAttributeValue();
}
