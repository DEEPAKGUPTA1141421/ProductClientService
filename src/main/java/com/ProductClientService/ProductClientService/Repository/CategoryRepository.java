package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.categoryLevel = :level")
    Optional<Category> findByNameAndLevel(@Param("name") String name, @Param("level") Category.Level level);

    Optional<Category> findFirstByExternalIdAndCategoryLevel(Integer externalId, Category.Level categoryLevel);

    List<CategoryProjection> findByCategoryLevel(Category.Level level);

    // Use parent.id directly (because parent is a Category object)
    List<Category> findByParentIdIn(List<UUID> parentIds);
}

/// hjuihui gyuhgyuy gyutguyu hyiuy unjj huijbhjgujhyhhihhuihhuihuhuihiuhhuih
/// huiu huihhjbgj hjgyujhjkhiuhyi hjguygu hjguyjgjhguyjbhjguyj