package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.Brand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    List<Brand> findByCategoryId(UUID categoryId); // 👈 fetch brands by category uuids

    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Brand> searchBrands(@Param("keyword") String keyword);
}
// y788uut6t67y78y8 87y78 y7y78 y7i8y78 yi7y8i