package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.CreateBrandRequest;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;

    public ApiResponse<Object> getBrandsByCategory(UUID categoryId) {
        List<Brand> brands = brandRepository.findByCategoryId(categoryId);
        return new ApiResponse<>(true, "Brands fetched", brands, 200);
    }

    public ApiResponse<Object> search(String keyword, UUID categoryId) {
        String normalized = normalize(keyword);

        List<Brand> result = brandRepository
                .findTop10ByCategoryIdAndApprovedTrueAndActiveTrueAndNormalisedNameContaining(
                        categoryId, normalized);

        return new ApiResponse<>(true, "Search results", result, 200);
    }

    public ApiResponse<Object> createBrand(CreateBrandRequest request) {
        String normalized = normalize(request.getName());

        Optional<Brand> existing = brandRepository.findByNormalisedNameAndCategoryId(
                normalized,
                request.getCategoryId());

        if (existing.isPresent()) {
            return new ApiResponse<>(true, "Brand already exists", existing.get(), 200);
        }

        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setNormalisedName(normalized);
        brand.setCategoryId(request.getCategoryId());
        brand.setApproved(false); // Seller created → needs approval

        Brand saved = brandRepository.save(brand);
        return new ApiResponse<>(true, "Brand created", saved, 201);
    }

    public ApiResponse<Object> pending() {
        List<Brand> pending = brandRepository.findByApprovedFalse();
        return new ApiResponse<>(true, "Pending brands fetched", pending, 200);
    }

    public ApiResponse<Object> delete(UUID id) {
        Brand brand = brandRepository.findById(id).orElseThrow();
        brand.setActive(false);
        brandRepository.save(brand);
        return new ApiResponse<>(true, "Brand deactivated", null, 200);
    }

    public ApiResponse<Object> approve(UUID id) {
        Brand brand = brandRepository.findById(id).orElseThrow();
        brand.setApproved(true);
        brandRepository.save(brand);
        return new ApiResponse<>(true, "Brand approved", null, 200);
    }

    private String normalize(String input) {
        return input.trim().toLowerCase();
    }
}