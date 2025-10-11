package com.ProductClientService.ProductClientService.Repository;

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
                    SELECT
                        p.id as product_id,
                        p.name,
                        p.description,
                        p.is_standard,
                        pv.id as variant_id,
                        pv.sku,
                        pv.price,
                        pv.discount_price,
                        pv.discount_percentage,
                        pv.stock,
                        (
                            SELECT pa.images
                            FROM product_attributes pa
                            INNER JOIN category_attributes ca
                                ON pa.category_attribute_id = ca.id
                            WHERE pa.product_id = p.id
                                AND ca.is_image_attribute = true
                            LIMIT 1
                        ) as image_url
                    FROM products p
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

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<UUID, ProductSearchDtoBuilder> dtoMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            UUID productId = (UUID) row[0];
            String name = (String) row[1];
            String description = (String) row[2];
            Boolean isStandard = (Boolean) row[3];
            UUID variantId = (UUID) row[4];
            String sku = (String) row[5];
            String price = row[6] != null ? row[6].toString() : null;
            String discountPrice = row[7] != null ? row[7].toString() : null;
            String discountPercentage = row[8] != null ? row[8].toString() : null;
            Integer stock = row[9] != null ? ((Number) row[9]).intValue() : null;
            String imageUrl = (String) row[10];

            dtoMap.computeIfAbsent(productId,
                    id -> new ProductSearchDtoBuilder(productId, name, description, isStandard, imageUrl));

            if (variantId != null) {
                dtoMap.get(productId).addVariant(
                        new VariantDto(variantId, sku, price, discountPrice, discountPercentage, stock));
            }
        }

        return dtoMap.values().stream()
                .map(ProductSearchDtoBuilder::build)
                .toList();
    }

    // ===================== DTOs ======================

    public record ProductSearchDto(
            UUID id,
            String name,
            String description,
            Boolean isStandard,
            String image,
            String price,
            boolean stockAvailable,
            VariantDto variant) {
    }

    public record VariantDto(
            UUID id,
            String sku,
            String price,
            String discountPrice,
            String discountPercentage,
            Integer stock) {
    }

    // ===================== Builder ======================
    private static class ProductSearchDtoBuilder {
        private final UUID id;
        private final String name;
        private final String description;
        private final Boolean isStandard;
        private final String image;
        private final List<VariantDto> variants = new ArrayList<>();

        ProductSearchDtoBuilder(UUID id, String name, String description, Boolean isStandard, String image) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.isStandard = isStandard;
            this.image = image;
        }

        void addVariant(VariantDto v) {
            variants.add(v);
        }

        ProductSearchDto build() {
            if (variants.isEmpty()) {
                return new ProductSearchDto(id, name, description, isStandard, image, null, false, null);
            }

            // Choose variant with minimum price (parse safely as Double)
            VariantDto minPriceVariant = variants.stream()
                    .filter(v -> v.price() != null)
                    .min(Comparator.comparingDouble(v -> {
                        try {
                            return Double.parseDouble(v.price());
                        } catch (NumberFormatException e) {
                            return Double.MAX_VALUE;
                        }
                    }))
                    .orElse(variants.get(0));

            boolean stockAvailable = variants.stream()
                    .anyMatch(v -> v.stock() != null && v.stock() > 0);

            return new ProductSearchDto(
                    id,
                    name,
                    description,
                    isStandard,
                    image,
                    minPriceVariant.price(),
                    stockAvailable,
                    minPriceVariant);
        }
    }
}

//hiuhu hyhhuuhu