package com.ProductClientService.ProductClientService.Service;

import java.util.List;

import com.ProductClientService.ProductClientService.Model.ProductVariant;

public interface StockObserver {
    void update(ProductVariant variant, List<String> emails);
}
