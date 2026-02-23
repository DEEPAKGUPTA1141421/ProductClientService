package com.ProductClientService.ProductClientService.Repository;

import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.Tag;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

   @Query(value = """
         SELECT * FROM tags
         WHERE
             (
                 length(:keyword) < 3
                 AND name ILIKE concat(:keyword, '%')
             )
         OR
             (
                 length(:keyword) >= 3
                 AND (
                     name ILIKE concat('%', :keyword, '%')
                     OR name % :keyword
                 )
             )
         ORDER BY
             CASE
                 WHEN name ILIKE concat(:keyword, '%') THEN 1
                 WHEN name ILIKE concat('%', :keyword, '%') THEN 2
                 ELSE 3
             END,
             similarity(name, :keyword) DESC
         """, nativeQuery = true)
   List<Tag> searchTags(@Param("keyword") String keyword);

   Optional<Tag> findByNameIgnoreCase(String name);
}
