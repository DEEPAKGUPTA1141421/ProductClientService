package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.CategoryAttribute;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, UUID> {
    Optional<CategoryAttribute> findByCategoryId(UUID categoryId);

    @Query("""
            SELECT DISTINCT ca FROM CategoryAttribute ca
            JOIN FETCH ca.attributes atr
            WHERE ca.category.id = :categoryId
            """)
    List<CategoryAttribute> findAllByCategoryId(UUID categoryId);
}
