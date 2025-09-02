package com.ProductClientService.ProductClientService.Service;

import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository;

public class ProductSearchBuilder {
    private final ProductSearchCriteria criteria = new ProductSearchCriteria();

    public ProductSearchBuilder category(UUID categoryId) {
        criteria.setCategoryId(categoryId);
        return this;
    }

    public ProductSearchBuilder brand(UUID brandId) {
        criteria.setBrandId(brandId);
        return this;
    }

    public ProductSearchBuilder seller(UUID sellerId) {
        criteria.setSellerId(sellerId);
        return this;
    }

    public ProductSearchBuilder attribute(String name, String value) {
        criteria.getAttributes().put(name, value);
        return this;
    }

    public List<Product> execute(ProductSearchRepository repo) {
        return repo.search(criteria);
    }
}
