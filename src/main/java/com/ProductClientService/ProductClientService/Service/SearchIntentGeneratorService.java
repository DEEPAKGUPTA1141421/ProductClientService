package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.admin.ProductAttributeForIntentProjection;
import com.ProductClientService.ProductClientService.Model.CategorySearchIntentRule;
import com.ProductClientService.ProductClientService.Model.SearchIntent;
import com.ProductClientService.ProductClientService.Repository.CategorySearchIntentRuleRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.SearchIntentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.Uuid;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchIntentGeneratorService
 * ─────────────────────────────
 * Builds search keywords + structured filter payloads for every live product.
 *
 * TWO entry points:
 * 1. @Scheduled cron – runs nightly, processes all products where
 * search_intent_created = false.
 * 2. generateForProduct(UUID) – called immediately when a product goes LIVE.
 *
 * Keyword patterns generated per product
 * ────────────────────────────────────────
 * Given a product in category "T-Shirt", brand "Puma", attributes:
 * gender=men, color=red, size=XL, material=cotton
 *
 * BRAND_CATEGORY → "Puma T-Shirt"
 * SIZE_BRAND_CATEGORY → "XL Puma T-Shirt"
 * BRAND_GENDER_CATEGORY → "Puma T-Shirt for men"
 * GENDER_CATEGORY → "T-Shirt for men"
 * COLOR_GENDER_CATEGORY → "Red T-Shirt for men"
 * COLOR_CATEGORY → "Red T-Shirt"
 * ATTRIBUTE_CATEGORY → "Cotton T-Shirt" (material)
 * SIZE_CATEGORY → "XL T-Shirt"
 *
 * Each entry stores a `filterPayload` JSON that the frontend sends back
 * to the product search API when the user taps this suggestion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentGeneratorService {

    private final ProductRepository productRepository;
    private final SearchIntentRepository searchIntentRepository;
    private final ObjectMapper objectMapper;
    private final CategorySearchIntentRuleRepository categorySearchIntentRuleRepository;
    // ─── Attribute name constants (case-insensitive matching)
    // ───────────────────
    private static final Set<String> GENDER_ATTRS = Set.of("gender", "for", "agegroup");
    private static final Set<String> COLOR_ATTRS = Set.of("color", "colour");
    private static final Set<String> SIZE_ATTRS = Set.of("size");
    private static final Set<String> MATERIAL_ATTRS = Set.of("material",
            "fabric");
    // Everything else is treated as a generic "attribute" suggestion

    // ── CRON: runs every night at 2 AM
    // ──────────────────────────────────────────

    // ── IMMEDIATE: called from SellerService when product goes LIVE
    // ─────────────

    @Transactional
    public void generateForProduct(UUID productId) {

        log.info("Generating search intents for product {}", productId);

        try {
            List<ProductAttributeForIntentProjection> rows = productRepository
                    .findAttributesForIntentByProductId(productId);

            if (rows.isEmpty()) {
                log.warn("No attribute rows found for product {}", productId);
                return;
            }

            ProductAttributeForIntentProjection first = rows.get(0);

            String brand = first.brandName();
            UUID brandId = first.brandId();
            String category = first.categoryName();
            UUID categoryId = first.categoryId();

            String base = category.toLowerCase();

            // 🔥 GROUP ATTRIBUTES (UNIQUE VALUES)
            Map<String, List<String>> attributes = rows.stream()
                    .filter(r -> r.attributeName() != null && r.attributeValue() != null)
                    .collect(Collectors.groupingBy(
                            (ProductAttributeForIntentProjection r) -> r.attributeName().toLowerCase().trim(),
                            LinkedHashMap::new,
                            Collectors.mapping(
                                    (ProductAttributeForIntentProjection r) -> r.attributeValue().toLowerCase().trim(),
                                    Collectors.collectingAndThen(
                                            Collectors.toCollection(LinkedHashSet::new),
                                            ArrayList::new))));

            List<CategorySearchIntentRule> rules = categorySearchIntentRuleRepository.findByCategoryId(categoryId);

            // 🔥 PROCESS RULES → PREFIX / SUFFIX
            Map<String, List<String>> prefixMap = new HashMap<>();
            Map<String, List<String>> suffixMap = new HashMap<>();

            processRules(attributes, rules, prefixMap, suffixMap);

            // 🔥 1. BASE INTENT
            saveIntent(base, buildBasePayload(categoryId));

            // 🔥 2. BRAND
            if (brand != null) {
                saveIntent(
                        brand.toLowerCase() + " " + base,
                        buildBrandPayload(categoryId, brandId));
            }

            // 🔥 3. ATTRIBUTE PREFIX INTENTS
            for (Map.Entry<String, List<String>> entry : prefixMap.entrySet()) {

                String attrName = entry.getKey();

                for (String val : entry.getValue()) {

                    String keyword = val + " " + base;

                    saveIntent(
                            keyword,
                            buildAttributePayload(categoryId, attrName, val));
                }
            }

            // 🔥 4. ATTRIBUTE SUFFIX INTENTS
            for (Map.Entry<String, List<String>> entry : suffixMap.entrySet()) {

                String attrName = entry.getKey();

                for (String val : entry.getValue()) {

                    String keyword = base + " " + val;

                    saveIntent(
                            keyword,
                            buildAttributePayload(categoryId, attrName, val));
                }
            }

            // 🔥 5. COMBINATIONS (PREFIX + SUFFIX)
            for (Map.Entry<String, List<String>> p : prefixMap.entrySet()) {
                for (String pv : p.getValue()) {

                    for (Map.Entry<String, List<String>> s : suffixMap.entrySet()) {
                        for (String sv : s.getValue()) {

                            String keyword = pv + " " + base + " " + sv;

                            Map<String, List<String>> combo = new HashMap<>();
                            combo.put(p.getKey(), List.of(pv));
                            combo.put(s.getKey(), List.of(sv.replaceAll("for ", "")));

                            saveIntent(
                                    keyword,
                                    buildMultiAttributePayload(categoryId, combo));
                        }
                    }
                }
            }

            // 🔥 6. PRICE INTENTS
            generatePriceIntents(categoryId, base);

            log.info("Search intents generated for product {}", productId);

        } catch (Exception e) {
            log.error("Failed to generate intents for product {}: {}", productId,
                    e.getMessage());
        }
    }

    private void processRules(
            Map<String, List<String>> attributes,
            List<CategorySearchIntentRule> rules,
            Map<String, List<String>> prefixMap,
            Map<String, List<String>> suffixMap) {

        for (CategorySearchIntentRule rule : rules) {

            String attrName = rule.getAttributeName().toLowerCase();

            List<String> values = attributes.get(attrName);
            if (values == null)
                continue;

            for (String val : values) {

                if (rule.getPosition() == CategorySearchIntentRule.Position.PREFIX) {
                    prefixMap.computeIfAbsent(attrName, k -> new ArrayList<>()).add(val);
                }

                else if (rule.getPosition() == CategorySearchIntentRule.Position.SUFFIX) {
                    String word = rule.getJoinWord() != null
                            ? rule.getJoinWord() + " " + val
                            : val;

                    suffixMap.computeIfAbsent(attrName, k -> new ArrayList<>()).add(word);
                }

                else if (rule.getPosition() == CategorySearchIntentRule.Position.INFIX) {
                    String word = rule.getJoinWord() + " " + val;
                    suffixMap.computeIfAbsent(attrName, k -> new ArrayList<>()).add(word);
                }
            }
        }
    }

    private JsonNode buildBasePayload(UUID categoryId) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("categoryId", categoryId.toString());

        return root;
    }

    private JsonNode buildBrandPayload(UUID categoryId, UUID brandId) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("categoryId", categoryId.toString());

        ObjectNode filters = mapper.createObjectNode();
        ArrayNode brandArr = mapper.createArrayNode();
        brandArr.add(brandId.toString());

        filters.set("brand", brandArr);

        root.set("filters", filters);

        return root;
    }

    private JsonNode buildAttributePayload(UUID categoryId, String attr, String value) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("categoryId", categoryId.toString());

        ObjectNode filters = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();
        arr.add(value);

        filters.set(attr, arr);

        root.set("filters", filters);

        return root;
    }

    private JsonNode buildMultiAttributePayload(
            UUID categoryId,
            Map<String, List<String>> attributes) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("categoryId", categoryId.toString());

        ObjectNode filters = mapper.createObjectNode();

        for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {

            ArrayNode arr = mapper.createArrayNode();
            entry.getValue().forEach(arr::add);

            filters.set(entry.getKey(), arr);
        }

        root.set("filters", filters);

        return root;
    }

    private JsonNode buildPricePayload(UUID categoryId, int price) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        root.put("categoryId", categoryId.toString());

        ObjectNode priceNode = mapper.createObjectNode();
        priceNode.put("lte", price);

        root.set("price", priceNode);

        return root;
    }

    private void generatePriceIntents(UUID categoryId, String base) {

        List<Integer> buckets = List.of(199, 399, 599, 999, 1499);

        for (Integer price : buckets) {

            String keyword = base + " under " + price;

            saveIntent(
                    keyword,
                    buildPricePayload(categoryId, price));
        }
    }

    private void saveIntent(String keyword, JsonNode payload) {

        try {

            SearchIntent intent = SearchIntent.builder()
                    .keyword(keyword.toLowerCase())
                    .imageUrl("DEFAULT_IMAGE") // later dynamic
                    .suggestionType("AUTO")
                    .filterPayload(payload)
                    .build();

            searchIntentRepository.save(intent);

        } catch (Exception e) {
            log.debug("Duplicate skipped: {}", keyword);
        }
    }
    // ── Core processing
    // ─────────────────────────────────────────────────────────

    private void processProduct(UUID productId,
            List<ProductAttributeForIntentProjection> rows) {

        // All rows share the same product meta – grab from first row
        ProductAttributeForIntentProjection meta = rows.get(0);

        String categoryName = safeLower(meta.categoryName());
        String categoryImage = "meta.categoryImageUrl()";
        UUID categoryId = meta.categoryId();
        UUID brandId = meta.brandId();
        String brandName = meta.brandName();
        String productImage = "meta.productImageUrl()";

        // Choose best image: product image > category image
        String displayImage = productImage != null ? productImage : categoryImage;

        // Collect attribute values by semantic type
        String gender = null;
        String color = null;
        String size = null;
        String material = null;
        List<String[]> genericAttrs = new ArrayList<>(); // [name, value]

        for (ProductAttributeForIntentProjection row : rows) {
            String attrName = row.attributeName() != null ? row.attributeName().trim().toLowerCase() : "";
            String attrValue = row.attributeValue() != null ? row.attributeValue().trim() : "";

            if (attrValue.isEmpty())
                continue;

            if (GENDER_ATTRS.contains(attrName)) {
                gender = attrValue;
            } else if (COLOR_ATTRS.contains(attrName)) {
                color = attrValue;
            } else if (SIZE_ATTRS.contains(attrName)) {
                size = attrValue;
            } else if (MATERIAL_ATTRS.contains(attrName)) {
                material = attrValue;
            } else {
                genericAttrs.add(new String[] { attrName, attrValue });
            }
        }

        // Build base filter payload (categoryId is always present)
        ObjectNode baseFilter = objectMapper.createObjectNode();
        baseFilter.put("categoryId", categoryId.toString());
        if (brandId != null)
            baseFilter.put("brandId", brandId.toString());
        if (gender != null)
            baseFilter.put("gender", gender);
        if (color != null)
            baseFilter.put("color", color);
        if (size != null)
            baseFilter.put("size", size);
        if (material != null)
            baseFilter.put("material", material);

        // ── 1. BRAND_CATEGORY → "Puma T-Shirt" ────────────────────────────────
        if (brandName != null) {
            String keyword = capitalize(brandName) + " " + categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("gender");
            filter.remove("color");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "BRAND_CATEGORY", filter, displayImage);
        }

        // ── 2. SIZE_BRAND_CATEGORY → "XL Puma Shoes" ──────────────────────────
        if (size != null && brandName != null) {
            String keyword = size.toUpperCase() + " " + capitalize(brandName) + " " +
                    categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("gender");
            filter.remove("color");
            filter.remove("material");
            saveIntent(keyword, "SIZE_BRAND_CATEGORY", filter, displayImage);
        }

        // ── 3. BRAND_GENDER_CATEGORY → "Puma Shoes for Men" ───────────────────
        if (brandName != null && gender != null) {
            String keyword = capitalize(brandName) + " " + categoryName + " for " +
                    safeLower(gender);
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("color");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "BRAND_GENDER_CATEGORY", filter, displayImage);
        }

        // ── 4. GENDER_CATEGORY → "Watches for Women" ──────────────────────────
        if (gender != null) {
            String keyword = categoryName + " for " + safeLower(gender);
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("color");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "GENDER_CATEGORY", filter, categoryImage != null ? categoryImage : displayImage);
        }

        // ── 5. COLOR_GENDER_CATEGORY → "Red T-Shirt for Kids" ─────────────────
        if (color != null && gender != null) {
            String keyword = capitalize(color) + " " + categoryName + " for " +
                    safeLower(gender);
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "COLOR_GENDER_CATEGORY", filter, displayImage);
        }

        // ── 6. COLOR_CATEGORY → "Red T-Shirt" ─────────────────────────────────
        if (color != null) {
            String keyword = capitalize(color) + " " + categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("gender");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "COLOR_CATEGORY", filter, displayImage);
        }

        // ── 7. SIZE_CATEGORY → "XL T-Shirt" ───────────────────────────────────
        if (size != null) {
            String keyword = size.toUpperCase() + " " + categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("gender");
            filter.remove("color");
            filter.remove("material");
            saveIntent(keyword, "SIZE_CATEGORY", filter, displayImage);
        }

        // ── 8. MATERIAL_CATEGORY → "Cotton T-Shirt" ───────────────────────────
        if (material != null) {
            String keyword = capitalize(material) + " " + categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("gender");
            filter.remove("color");
            filter.remove("size");
            saveIntent(keyword, "ATTRIBUTE_CATEGORY", filter, displayImage);
        }

        // ── 9. GENERIC ATTRIBUTE_CATEGORY → e.g. "Waterproof Jacket" ──────────
        for (String[] attr : genericAttrs) {
            String attrName = attr[0];
            String attrValue = attr[1];
            String keyword = capitalize(attrValue) + " " + categoryName;

            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("brandId");
            filter.remove("gender");
            filter.remove("color");
            filter.remove("size");
            filter.remove("material");
            filter.put("attributeName", attrName);
            filter.put("attributeValue", attrValue);

            saveIntent(keyword, "ATTRIBUTE_CATEGORY", filter, displayImage);
        }

        // ── 10. BRAND_COLOR_CATEGORY → "Puma Red Shoes" ───────────────────────
        if (brandName != null && color != null) {
            String keyword = capitalize(brandName) + " " + capitalize(color) + " " +
                    categoryName;
            ObjectNode filter = baseFilter.deepCopy();
            filter.remove("gender");
            filter.remove("size");
            filter.remove("material");
            saveIntent(keyword, "BRAND_COLOR_CATEGORY", filter, displayImage);
        }

        // ── 11. BRAND_SIZE_GENDER_CATEGORY → "Puma XL Shoes for Men" ──────────
        if (brandName != null && size != null && gender != null) {
            String keyword = capitalize(brandName) + " " + size.toUpperCase()
                    + " " + categoryName + " for " + safeLower(gender);
            saveIntent(keyword, "BRAND_SIZE_GENDER_CATEGORY", baseFilter.deepCopy(),
                    displayImage);
        }
    }

    // ── Persistence helper
    // ───────────────────────────────────────────────────────

    private void saveIntent(String keyword, String type,
            ObjectNode filterPayload, String imageUrl) {
        if (keyword == null || keyword.isBlank())
            return;

        keyword = keyword.trim();

        // Idempotent: skip if keyword already exists
        if (searchIntentRepository.existsByKeyword(keyword))
            return;

        try {
            SearchIntent intent = SearchIntent.builder()
                    .keyword(keyword)
                    .suggestionType(type)
                    .filterPayload(filterPayload)
                    .imageUrl(imageUrl != null ? imageUrl : "")
                    .searchCount(0L)
                    .clickCount(0L)
                    .build();

            searchIntentRepository.save(intent);
            log.debug("Saved intent: [{}] → {}", type, keyword);

        } catch (Exception e) {
            // Unique constraint violation is expected if another thread raced us
            log.debug("Skipped duplicate intent: {}", keyword);
        }
    }

    // ── Utilities
    // ────────────────────────────────────────────────────────────────

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank())
            return value;
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase() +
                trimmed.substring(1).toLowerCase();
    }
}
// jiooiiuonju9h iuioljijnjkhjjiljjklnjlknkjkbbhuhuhbhhu
// khhukbhuuhkkhhukuhhjkhu
// joihukbiujjkkjjnjioijoinuiui jiuhiuhuihukhhkhnjjkhui uy ygujyygjyuuhiuhg
// guihuhyhbhuhukjikhukhukhuk hukjkhukhunjkhjhhukh uhih huhk huu hukhuhbjhj
// htfghy tg hgtgy thfygt tyfttftt gytyg