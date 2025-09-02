package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BrandService {
    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public List<Brand> getBrandsByCategory(UUID categoryId) {
        return brandRepository.findByCategoryId(categoryId);
    }
}