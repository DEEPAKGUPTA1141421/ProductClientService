package com.ProductClientService.ProductClientService.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.AttributeDto;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AttributeRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public List<AttributeDto> findFiltersByCategoryId(UUID categoryId) {
        String sql = """
                SELECT
                    a.id,
                    a.name,
                    a.field_type,
                    a.is_required,
                    ARRAY_TO_STRING(a.options, ',') as options
                FROM categories c
                JOIN category_attributes ca ON ca.category_id = c.id
                JOIN category_attribute_mapping cam ON cam.category_attribute_id = ca.id
                JOIN attributes a ON a.id = cam.attribute_id
                WHERE c.id = :categoryId
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categoryId", categoryId);

        List<Object[]> rows = query.getResultList();

        return rows.stream().map(row -> {
            UUID id = (UUID) row[0];
            String name = (String) row[1];
            String fieldType = (String) row[2];
            Boolean isRequired = row[3] != null ? (Boolean) row[3] : true;

            // Convert comma-separated string into list
            List<String> options = new ArrayList<>();
            if (row[4] != null) {
                String optionsStr = (String) row[4];
                options = Arrays.asList(optionsStr.split(","));
            }

            return AttributeDto.builder()
                    .id(id)
                    .name(name)
                    .fieldType(fieldType)
                    .isRequired(isRequired)
                    .options(options)
                    .build();
        }).collect(Collectors.toList());
    }
}
// hiuhyu hy huui iu hhyubhyihu8u iuuu huihuiby huyyh yiyyiiu