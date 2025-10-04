package com.ProductClientService.ProductClientService.Service;

import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository;
import com.ProductClientService.ProductClientService.Repository.ProductSearchRepository.ProductSearchDto;

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

    public List<ProductSearchDto> execute(ProductSearchRepository repo) {
        return repo.search(criteria);
    }
}
// hyy7ed guyt67etd gyut67edt gutr7rde hgfyedgttedy biyh7uy8i gyutyu