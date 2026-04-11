package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.Cart.CategoryTreeProjection;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.categoryLevel = :level")
    Optional<Category> findByNameAndLevel(@Param("name") String name, @Param("level") Category.Level level);

    Optional<Category> findFirstByExternalIdAndCategoryLevel(Integer externalId, Category.Level categoryLevel);

    List<CategoryProjection> findByCategoryLevel(Category.Level level);

    Optional<Category> findByIdAndCategoryLevel(UUID Id, Category.Level level);

    // Use parent.id directly (because parent is a Category object)
    List<Category> findByParentIdIn(List<UUID> parentIds);

    @Query("""
                SELECT c.id as id,
                       c.name as name,
                       c.imageUrl as imageUrl,
                       c.categoryLevel as categoryLevel,
                       c.parent.id as parent_Id
                FROM Category c
            """)
    List<CategoryTreeProjection> fetchCategoryTreeData();

    /**
     * Returns the parent category's UUID for the given category, or empty if it has no parent.
     * Used by CategoryFilterService to walk up the hierarchy without loading the full entity.
     */
    @Query("SELECT c.parent.id FROM Category c WHERE c.id = :id AND c.parent IS NOT NULL")
    Optional<UUID> findParentIdById(@Param("id") UUID id);
}

/// hjuihui gyuhgyuy gyutguyu hyiuy unjj huijbhjgujhyhhihhuihhuihuhuihiuhhui
/// guyyifrbhyif hyiyiufe ghiuyif hiuyif hiyyif yi7yifrhuiyhuyhiuyhuhuiufr
/// hkhubbyyiyhhukuhuh gyybhuku huiuuuujhyyy khuhukuhhuu gyjhyuuy hiuhui huhuj
/// hgbj bhh mkbhj jhbbh bhknhjk bknnjnj