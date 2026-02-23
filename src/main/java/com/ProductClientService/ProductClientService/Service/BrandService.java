package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.CreateBrandRequest;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.Optional;
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

    public List<Brand> search(String keyword, UUID categoryId) {

        String normalized = normalize(keyword);

        return brandRepository
                .findTop10ByCategoryIdAndApprovedTrueAndActiveTrueAndNormalisedNameContaining(
                        categoryId, normalized);
    }

    public Brand createBrand(CreateBrandRequest request) {

        String normalized = normalize(request.getName());

        Optional<Brand> existing = brandRepository.findByNormalisedNameAndCategoryId(
                normalized,
                request.getCategoryId());

        if (existing.isPresent()) {
            return existing.get();
        }

        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setNormalisedName(normalized);
        brand.setCategoryId(request.getCategoryId());
        brand.setApproved(false); // Seller created → needs approval

        return brandRepository.save(brand);
    }

    public List<Brand> pending() {
        return brandRepository.findByApprovedFalse();
    }

    public void delete(UUID id) {
        Brand brand = brandRepository.findById(id).orElseThrow();
        brand.setActive(false);
        brandRepository.save(brand);
    }

    public void approve(UUID id) {
        Brand brand = brandRepository.findById(id).orElseThrow();
        brand.setApproved(true);
        brandRepository.save(brand);
    }

    private String normalize(String input) {
        return input.trim().toLowerCase();
    }
}