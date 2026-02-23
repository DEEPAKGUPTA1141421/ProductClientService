package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ProductAttributeSuggestionProjection;
import com.ProductClientService.ProductClientService.DTO.ProductPriceSuggestionProjection;
import com.ProductClientService.ProductClientService.DTO.ProductSuggestionProjection;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.Model.SearchIntent;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.SearchIntentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionGeneratorService {

    private final ProductRepository productRepository;
    private final SearchIntentRepository searchIntentRepository;

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void generateCompositeSuggestions() {

        log.info("Starting composite suggestion generation...");

        generateBrandSuggestions();
        generateAttributeSuggestions();
        generatePriceSuggestions();

        productRepository.markAllSearchIntentCreated();

        log.info("Suggestion generation completed.");
    }

    // =====================================================
    // BRAND SUGGESTIONS
    // =====================================================

    private void generateBrandSuggestions() {

        List<ProductSuggestionProjection> products = productRepository.findProductsForSuggestion();

        for (ProductSuggestionProjection product : products) {

            if (product.getBrandName() == null)
                continue;

            String keyword = capitalize(product.getBrandName()) + " " +
                    safeLower(product.getCategoryName());

            saveKeyword(keyword, "BRAND_CATEGORY");
        }
    }

    // =====================================================
    // ATTRIBUTE SUGGESTIONS
    // =====================================================

    private void generateAttributeSuggestions() {

        List<ProductAttributeSuggestionProjection> attributes = productRepository.findAttributeSuggestions();

        // Group attributes by product
        Map<UUID, List<ProductAttributeSuggestionProjection>> grouped = attributes.stream()
                .collect(Collectors.groupingBy(
                        ProductAttributeSuggestionProjection::getProductId));

        for (List<ProductAttributeSuggestionProjection> productAttrs : grouped.values()) {

            String categoryName = safeLower(productAttrs.get(0).getCategoryName());

            // Extract age once
            String age = productAttrs.stream()
                    .filter(a -> a.getAttributeName().equalsIgnoreCase("age"))
                    .map(ProductAttributeSuggestionProjection::getAttributeValue)
                    .findFirst()
                    .orElse(null);

            String ageSuffix = (age != null && !age.isBlank())
                    ? " for " + safeLower(age)
                    : "";

            for (ProductAttributeSuggestionProjection attr : productAttrs) {

                String attrName = attr.getAttributeName();
                String attrValue = attr.getAttributeValue();

                if (attrName == null || attrValue == null)
                    continue;

                String keyword = null;

                switch (attrName.toLowerCase()) {

                    case "size":
                        keyword = safeLower(attrValue) + " " + categoryName + ageSuffix;
                        break;

                    case "color":
                        keyword = capitalize(attrValue) + " " + categoryName + ageSuffix;
                        break;

                    case "age":
                        keyword = categoryName + " for " + safeLower(attrValue);
                        break;

                    default:
                        keyword = safeLower(attrValue) + " " + categoryName + ageSuffix;
                }

                saveKeyword(keyword, "ATTRIBUTE_CATEGORY");
            }
        }
    }

    // =====================================================
    // PRICE SUGGESTIONS
    // =====================================================

    private void generatePriceSuggestions() {

        List<ProductPriceSuggestionProjection> prices = productRepository.findPriceSuggestions();

        for (ProductPriceSuggestionProjection price : prices) {

            if (price.getMinPrice() == null)
                continue;

            int bucket = calculatePriceBucket(new BigDecimal(price.getMinPrice()));

            String keyword = safeLower(price.getCategoryName()) +
                    " under " + bucket;

            saveKeyword(keyword, "PRICE_CATEGORY");
        }
    }

    // =====================================================
    // SAVE METHOD (Duplicate Safe)
    // =====================================================

    private void saveKeyword(String keyword, String type) {

        if (keyword == null || keyword.isBlank())
            return;

        keyword = keyword.trim().toLowerCase();

        try {

            if (!searchIntentRepository.existsByKeyword(keyword)) {

                SearchIntent intent = new SearchIntent();
                intent.setKeyword(keyword);
                intent.setSuggestionType(type);

                searchIntentRepository.save(intent);
            }

        } catch (Exception e) {
            log.warn("Duplicate or error for keyword: {}", keyword);
        }
    }

    // =====================================================
    // PRICE BUCKET LOGIC
    // =====================================================

    private int calculatePriceBucket(BigDecimal price) {

        int value = price.intValue();

        int rounded = (int) (Math.ceil(value / 100.0) * 100);

        return rounded - 1;
    }

    // =====================================================
    // UTILITIES
    // =====================================================

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank())
            return value;
        return value.substring(0, 1).toUpperCase()
                + value.substring(1).toLowerCase();
    }
}
