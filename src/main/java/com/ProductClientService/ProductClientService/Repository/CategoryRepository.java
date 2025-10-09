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

import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Repository.Projection.CategoryProjection;

import jakarta.transaction.Transactional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);

    @Query("SELECT c FROM Category c WHERE c.name = :name AND c.categoryLevel = :level")
    Optional<Category> findByNameAndLevel(@Param("name") String name, @Param("level") Category.Level level);

    Optional<Category> findFirstByExternalIdAndCategoryLevel(Integer externalId, Category.Level categoryLevel);

    List<CategoryProjection> findByCategoryLevel(Category.Level level);

    // Use parent.id directly (because parent is a Category object)
    List<Category> findByParentIdIn(List<UUID> parentIds);

    @Query(value = """
            SELECT
                p.id AS id,
                p.name AS name,
                p.priority AS priority,
                p.image_url AS imageUrl,
                COALESCE(
                    (
                        SELECT json_agg(category_data)::jsonb
                        FROM (
                            SELECT
                                c.id,
                                c.name,
                                c.priority,
                                c.image_url AS imageUrl,
                                COALESCE(
                                    (
                                        SELECT json_agg(sub_child_data)::jsonb
                                        FROM (
                                            SELECT
                                                sc.id,
                                                sc.name,
                                                sc.priority,
                                                sc.image_url AS imageUrl
                                            FROM categories sc
                                            WHERE sc.parent_id = c.id
                                            ORDER BY sc.priority ASC
                                            LIMIT 10
                                        ) AS sub_child_data
                                    ),
                                    '[]'::jsonb
                                ) AS children
                            FROM categories c
                            WHERE c.parent_id = p.id
                            ORDER BY c.priority ASC
                            LIMIT 10
                        ) AS category_data
                    ),
                    '[]'::jsonb
                ) AS children
            FROM categories p
            WHERE p.category_level = CAST(:level AS smallint)
            ORDER BY p.priority ASC
            LIMIT 10
            """, nativeQuery = true)
    List<Map<String, Object>> findTop10ParentWithChildren(@Param("level") short level);

    @Query(value = """
            SELECT
                c.id AS id,
                c.name AS name,
                c.priority AS priority,
                c.image_url AS imageUrl
            FROM categories c
            WHERE c.category_level = CAST(:level AS smallint)
            ORDER BY c.priority ASC
            LIMIT 10
            """, nativeQuery = true)
    List<CategoryProjection> findTop10ByLevel(@Param("level") short level);

}

/// hjuihui gyuhgyuy gyutguyu hyiuy unjj huijbhjgujhyhhihhuihhuihuhuihiuhhui
/// guyyifrbhyif hyiyiufe ghiuyif hiuyif hiyyif yi7yifrhuiyhuyhiuyhuhuiufr
/// hkhubbyyiyhhukuhuh gyybhuku huiuuuujhyyy khuhukuhhuu gyjhyuuy hiuhui