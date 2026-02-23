package com.ProductClientService.ProductClientService.DTO;

import java.util.UUID;

public interface ProductSuggestionProjection {

    UUID getId();

    String getCategoryName();

    String getBrandName();

    UUID getBrandId();

    Boolean getSearchIntentCreated();
}
