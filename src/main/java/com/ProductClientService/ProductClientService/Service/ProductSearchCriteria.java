package com.ProductClientService.ProductClientService.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchCriteria {
    private UUID categoryId;
    private UUID brandId;
    private UUID sellerId;
    private Map<String, String> attributes = new HashMap<>();
    private String lowestPrice;
    private String highestPrice;
    private Map<String, Integer> discount = new HashMap<>();
}
