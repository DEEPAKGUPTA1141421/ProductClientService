package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.CategoryFilterMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryFilterMappingRepository extends JpaRepository<CategoryFilterMapping, UUID> {

    /** Active mappings for a category, ordered for rendering. */
    List<CategoryFilterMapping> findByCategoryIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID categoryId);

    /** All mappings for admin management (including inactive). */
    List<CategoryFilterMapping> findByCategoryIdOrderByDisplayOrderAsc(UUID categoryId);

    boolean existsByCategoryIdAndFilterId(UUID categoryId, UUID filterId);

    Optional<CategoryFilterMapping> findByCategoryIdAndFilterId(UUID categoryId, UUID filterId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CategoryFilterMapping m WHERE m.category.id = :categoryId AND m.filter.id = :filterId")
    void deleteByCategoryIdAndFilterId(@Param("categoryId") UUID categoryId,
                                       @Param("filterId") UUID filterId);
}
