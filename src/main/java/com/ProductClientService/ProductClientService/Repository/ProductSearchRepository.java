package com.ProductClientService.ProductClientService.Repository;

import java.time.ZonedDateTime;
import java.util.*;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Service.ProductSearchCriteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class ProductSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<ProductSearchDto> search(ProductSearchCriteria criteria) {

        StringBuilder sql = new StringBuilder("""
                    SELECT p.id as product_id,
                           p.name,
                           p.description,
                           p.is_standard,
                           p.created_at,
                           p.updated_at,
                           pv.id as variant_id,
                           pv.sku,
                           pv.price,
                           pv.stock,
                           pv.created_at as variant_created_at,
                           pv.updated_at as variant_updated_at
                    FROM products p
                    LEFT JOIN product_attributes pa ON p.id = pa.product_id
                    LEFT JOIN product_variants pv ON p.id = pv.product_id
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
                sql.append("""
                        AND EXISTS (
                            SELECT 1
                            FROM product_attribute pa2
                            JOIN attribute a2 ON pa2.attribute_id = a2.id
                            WHERE pa2.product_id = p.id
                            AND a2.name = :attrName""" + i +
                        " AND pa2.value = :attrValue" + i + ")");
                params.put("attrName" + i, entry.getKey());
                params.put("attrValue" + i, entry.getValue());
                i++;
            }
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        // Group results by product
        Map<UUID, ProductSearchDtoBuilder> dtoMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            UUID productId = (UUID) row[0];
            String name = (String) row[1];
            String description = (String) row[2];
            Boolean isStandard = (Boolean) row[3];
            // row[4] = product created_at, row[5] = product updated_at (if needed)

            UUID variantId = (UUID) row[6];
            String sku = (String) row[7];
            String price = (String) row[8];
            Integer stock = row[9] != null ? ((Number) row[9]).intValue() : null;
            // ZonedDateTime variantCreatedAt = (ZonedDateTime) row[10];
            // ZonedDateTime variantUpdatedAt = (ZonedDateTime) row[11];

            dtoMap.computeIfAbsent(productId,
                    id -> new ProductSearchDtoBuilder(productId, name, description, isStandard));

            if (variantId != null) {
                dtoMap.get(productId).addVariant(
                        new VariantDto(variantId, sku, price, stock));
            }
        }

        return dtoMap.values().stream()
                .map(ProductSearchDtoBuilder::build)
                .toList();
    }

    // --- DTOs ---
    public record ProductSearchDto(
            UUID id,
            String name,
            String description,
            Boolean isStandard,
            List<VariantDto> variants) {
    }

    public record VariantDto(
            UUID id,
            String sku,
            String price,
            int stock) {
    }

    // --- Helper Builder for grouping ---
    private static class ProductSearchDtoBuilder {
        private final UUID id;
        private final String name;
        private final String description;
        private final Boolean isStandard;
        private final List<VariantDto> variants = new ArrayList<>();

        ProductSearchDtoBuilder(UUID id, String name, String description, Boolean isStandard) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.isStandard = isStandard;
        }

        void addVariant(VariantDto v) {
            variants.add(v);
        }

        ProductSearchDto build() {
            return new ProductSearchDto(id, name, description, isStandard, variants);
        }
    }
}

// hhuihuihuih ihyuiyui kjnihuihiuihuiuyhihyiiojoilnjhiuj gyiuh jhhiuh gyiuh gyugyu ggyty