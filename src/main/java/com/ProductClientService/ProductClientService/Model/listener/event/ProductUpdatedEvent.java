package com.ProductClientService.ProductClientService.Model.listener.event;

import com.ProductClientService.ProductClientService.Model.Product;

public class ProductUpdatedEvent {
    private final Product product;

    public ProductUpdatedEvent(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }
}