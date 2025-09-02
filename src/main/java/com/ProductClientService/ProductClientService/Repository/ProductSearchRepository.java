package com.ProductClientService.ProductClientService.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Service.ProductSearchCriteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class ProductSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Product> search(ProductSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                    SELECT DISTINCT p.*
                    FROM product p
                    LEFT JOIN product_attribute pa ON p.id = pa.product_id
                    LEFT JOIN attribute a ON pa.attribute_id = a.id
                    WHERE 1=1
                """);
        Map<String, Object> params = new HashMap<>();
        if (criteria.getCategoryId() != null) {
            sql.append(" AND p.category_id = :categoryId");
            params.put("categoryId", criteria.getCategoryId());
        }

        if (criteria.getBrandId() != null) {
            sql.append(" AND p.brand_id = :brandId");
            params.put("brandId", criteria.getBrandId());
        }

        if (criteria.getSellerId() != null) {
            sql.append(" AND p.seller_id = :sellerId");
            params.put("sellerId", criteria.getSellerId());
        }

        if (criteria.getAttributes() != null && !criteria.getAttributes().isEmpty()) {
            int i = 0;
            for (Map.Entry<String, String> entry : criteria.getAttributes().entrySet()) {
                sql.append(" AND EXISTS ( " +
                        "SELECT 1 FROM product_attribute pa2 " +
                        "JOIN attribute a2 ON pa2.attribute_id = a2.id " +
                        "WHERE pa2.product_id = p.id " +
                        "AND a2.name = :attrName" + i +
                        " AND pa2.value = :attrValue" + i + ")");
                params.put("attrName" + i, entry.getKey());
                params.put("attrValue" + i, entry.getValue());
                i++;
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Product.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }
}