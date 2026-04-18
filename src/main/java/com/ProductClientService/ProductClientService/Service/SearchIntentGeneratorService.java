package com.ProductClientService.ProductClientService.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.OpType;
import com.ProductClientService.ProductClientService.DTO.admin.ProductAttributeForIntentProjection;
import com.ProductClientService.ProductClientService.DTO.search.SearchIntentDocument;
import com.ProductClientService.ProductClientService.Model.CategorySearchIntentRule;
import com.ProductClientService.ProductClientService.Repository.CategorySearchIntentRuleRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ProductVariantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SearchIntentGeneratorService
 * ─────────────────────────────
 * Builds search keywords + structured filter payloads for every live product
 * and indexes them directly into the "search-intents-v1" Elasticsearch index.
 *
 * No database table involved — ES is the sole store for search intents.
 *
 * Entry point: generateForProduct(UUID)
 * Called by SearchIntentIndexerConsumer when it receives a product.live Kafka
 * event.
 *
 * Idempotency:
 * Each intent is indexed with op_type=create. If the keyword doc already exists
 * (from another product in the same category), the create is a no-op and the
 * existing doc — with its accumulated searchCount/clickCount — is preserved.
 *
 * Keyword patterns generated per product
 * ────────────────────────────────────────
 * Given category "T-Shirt", brand "Puma", attributes: gender=men, color=red,
 * size=XL
 *
 * BASE → "t-shirt"
 * BRAND_CATEGORY → "puma t-shirt"
 * PREFIX intents → "red t-shirt", "xl t-shirt" (PREFIX rules)
 * SUFFIX intents → "t-shirt for men" (SUFFIX rules)
 * COMBO intents → "red t-shirt for men" (PREFIX + SUFFIX)
 * PRICE intents → "t-shirt under 199", "... 399" … "… 1499"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIntentGeneratorService {

    static final String SEARCH_INTENTS_INDEX = "search-intents-v1";

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategorySearchIntentRuleRepository categorySearchIntentRuleRepository;
    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    // ── Entry point
    // ───────────────────────────────────────────────────────────────

    public void generateForProduct(UUID productId) {
        log.info("Generating search intents for product {}", productId);
        try {
            List<ProductAttributeForIntentProjection> rows = productRepository
                    .findAttributesForIntentByProductId(productId);
            log.info("Found {} attribute rows for product {}", rows.size(), productId);
            if (rows.isEmpty()) {
                log.warn("No attribute rows found for product {}", productId);
                return;
            }
            log.info("Attribute rows for product {}: {}", productId, rows.size());
            ProductAttributeForIntentProjection first = rows.get(0);
            String brand = first.getBrandName();
            UUID brandId = first.getBrandId();
            String category = first.getCategoryName();
            UUID categoryId = first.getCategoryId();
            String base = category.toLowerCase();

            // Group attribute values by attribute name (deduped, order-stable)
            Map<String, List<String>> attributes = rows.stream()
                    .filter(r -> r.getAttributeName() != null && r.getAttributeValue() != null)
                    .collect(Collectors.groupingBy(
                            r -> r.getAttributeName().toLowerCase().trim(),
                            LinkedHashMap::new,
                            Collectors.mapping(
                                    r -> r.getAttributeValue().toLowerCase().trim(),
                                    Collectors.collectingAndThen(
                                            Collectors.toCollection(LinkedHashSet::new),
                                            ArrayList::new))));

            List<CategorySearchIntentRule> rules = categorySearchIntentRuleRepository.findByCategoryId(categoryId);

            Map<String, List<String>> prefixMap = new HashMap<>();
            Map<String, List<String>> suffixMap = new HashMap<>();
            processRules(attributes, rules, prefixMap, suffixMap);

            // 1. Base intent: "t-shirt"
            indexIntent(base, buildBasePayload(categoryId));

            // 2. Brand intent: "puma t-shirt"
            if (brand != null) {
                indexIntent(brand.toLowerCase() + " " + base, buildBrandPayload(categoryId, brandId));
            }

            // 3. Prefix intents: "<attrValue> <base>"
            for (Map.Entry<String, List<String>> entry : prefixMap.entrySet()) {
                for (String val : entry.getValue()) {
                    indexIntent(val + " " + base,
                            buildAttributePayload(categoryId, entry.getKey(), val));
                }
            }

            // 4. Suffix intents: "<base> <joinedVal>"
            for (Map.Entry<String, List<String>> entry : suffixMap.entrySet()) {
                for (String val : entry.getValue()) {
                    indexIntent(base + " " + val,
                            buildAttributePayload(categoryId, entry.getKey(), val));
                }
            }

            // 5. Combination intents: "<prefixVal> <base> <suffixVal>"
            for (Map.Entry<String, List<String>> p : prefixMap.entrySet()) {
                for (String pv : p.getValue()) {
                    for (Map.Entry<String, List<String>> s : suffixMap.entrySet()) {
                        for (String sv : s.getValue()) {
                            String keyword = pv + " " + base + " " + sv;
                            Map<String, List<String>> combo = new HashMap<>();
                            combo.put(p.getKey(), List.of(pv));
                            combo.put(s.getKey(), List.of(sv.replaceAll("for ", "")));
                            indexIntent(keyword, buildMultiAttributePayload(categoryId, combo));
                        }
                    }
                }
            }

            // 6. Single price intent: "t-shirt under <floor(minPrice, 100)>"
            generatePriceIntent(productId, categoryId, base);

            log.info("Search intents indexed to ES for product {}", productId);

        } catch (Exception e) {
            log.error("Failed to generate intents for product {}: {}", productId, e.getMessage());
        }
    }

    // ── Rule processing
    // ───────────────────────────────────────────────────────────

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
                } else {
                    // SUFFIX and INFIX both produce suffix-style phrases
                    String word = rule.getJoinWord() != null
                            ? rule.getJoinWord() + " " + val
                            : val;
                    suffixMap.computeIfAbsent(attrName, k -> new ArrayList<>()).add(word);
                }
            }
        }
    }

    // ── ES indexing
    // ───────────────────────────────────────────────────────────────

    /**
     * Index intent with op_type=create — preserves existing docs (and their
     * counts).
     * Silently skips duplicate keywords.
     */
    private void indexIntent(String keyword, JsonNode payload) {
        if (keyword == null || keyword.isBlank())
            return;
        final String kw = keyword.trim().toLowerCase(); // final — safe for lambda capture

        try {
            Map<String, Object> payloadMap = objectMapper.convertValue(payload,
                    new TypeReference<Map<String, Object>>() {
                    });

            SearchIntentDocument doc = SearchIntentDocument.builder()
                    .keyword(kw)
                    .imageUrl("")
                    .suggestionType("AUTO")
                    .filterPayload(payloadMap)
                    .searchCount(0L)
                    .clickCount(0L)
                    .build();

            esClient.index(i -> i
                    .index(SEARCH_INTENTS_INDEX)
                    .id(kw)
                    .opType(OpType.Create) // no-op if doc already exists
                    .document(doc));
            log.debug("Indexed search intent: {}", kw);

        } catch (Exception e) {
            // 409 = doc already exists (another product shares the same keyword) — expected
            log.debug("Skipping intent '{}': {}", kw, e.getMessage());
        }
    }

    // ── Payload builders
    // ──────────────────────────────────────────────────────────

    private JsonNode buildBasePayload(UUID categoryId) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("categoryId", categoryId.toString());
        return root;
    }

    private JsonNode buildBrandPayload(UUID categoryId, UUID brandId) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("categoryId", categoryId.toString());
        ObjectNode filters = objectMapper.createObjectNode();
        ArrayNode brandArr = objectMapper.createArrayNode();
        brandArr.add(brandId.toString());
        filters.set("brand", brandArr);
        root.set("filters", filters);
        return root;
    }

    private JsonNode buildAttributePayload(UUID categoryId, String attr, String value) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("categoryId", categoryId.toString());
        ObjectNode filters = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(value);
        filters.set(attr, arr);
        root.set("filters", filters);
        return root;
    }

    private JsonNode buildMultiAttributePayload(UUID categoryId, Map<String, List<String>> attrs) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("categoryId", categoryId.toString());
        ObjectNode filters = objectMapper.createObjectNode();
        for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
            ArrayNode arr = objectMapper.createArrayNode();
            entry.getValue().forEach(arr::add);
            filters.set(entry.getKey(), arr);
        }
        root.set("filters", filters);
        return root;
    }

    private JsonNode buildPricePayload(UUID categoryId, int price) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("categoryId", categoryId.toString());
        ObjectNode priceNode = objectMapper.createObjectNode();
        priceNode.put("lte", price);
        root.set("price", priceNode);
        return root;
    }

    /**
     * Generates exactly one price intent for the product.
     * Bucket = floor(minVariantPrice / 100) * 100
     * e.g. price=450 → "t-shirt under 400"
     * price=850 → "t-shirt under 800"
     * Skipped if no variants exist or price cannot be parsed.
     */
    private void generatePriceIntent(UUID productId, UUID categoryId, String base) {
        variantRepository.findByProductId(productId).stream()
                .map(v -> {
                    try {
                        return Double.parseDouble(v.getPrice());
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(p -> p != null && p > 0)
                .min(Double::compareTo)
                .ifPresent(minPrice -> {
                    int bucket = (int) (Math.floor(minPrice / 100) * 100);
                    if (bucket > 0) {
                        indexIntent(base + " under " + bucket, buildPricePayload(categoryId, bucket));
                    }
                });
    }

    // ── Utilities
    // ─────────────────────────────────────────────────────────────────

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank())
            return value;
        String t = value.trim();
        return t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
    }
}
