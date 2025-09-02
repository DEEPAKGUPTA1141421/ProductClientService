package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    List<Brand> findByCategoryId(UUID categoryId); // 👈 fetch brands by category uuids
}