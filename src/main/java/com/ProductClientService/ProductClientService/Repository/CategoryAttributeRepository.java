package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.CategoryAttribute;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {
    Optional<CategoryAttribute> findByCategoryId(UUID categoryId);
}
