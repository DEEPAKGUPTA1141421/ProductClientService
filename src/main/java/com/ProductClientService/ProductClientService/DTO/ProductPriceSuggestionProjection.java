package com.ProductClientService.ProductClientService.DTO;

import java.util.UUID;

public interface ProductPriceSuggestionProjection {

    UUID getProductId();

    String getCategoryName();

    String getMinPrice();
}
