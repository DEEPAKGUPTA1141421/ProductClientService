package com.ProductClientService.ProductClientService.Service.shopify;

import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyIngestionResultDto;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyProductsResponse;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyProductsResponse.ShopifyImage;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyProductsResponse.ShopifyOption;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyProductsResponse.ShopifyProduct;
import com.ProductClientService.ProductClientService.DTO.shopify.ShopifyProductsResponse.ShopifyVariant;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ProductClientService.ProductClientService.Model.Brand;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductAttribute;
import com.ProductClientService.ProductClientService.Model.ProductMedia;
import com.ProductClientService.ProductClientService.Model.ProductVariant;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.StandardProduct;
import com.ProductClientService.ProductClientService.Model.Tag;
import com.ProductClientService.ProductClientService.Repository.BrandRepository;
import com.ProductClientService.ProductClientService.Repository.CategoryAttributeRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.StandardProductRepository;
import com.ProductClientService.ProductClientService.Repository.TagRepository;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyIngestionService {

    private static final String SHOPIFY_PRODUCTS_URL = "https://mithilafoods.com/products.json?limit=5";

    private final SellerRepository sellerRepository;
    private final StandardProductRepository standardProductRepository;
    private final ProductRepository productRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final TagRepository tagRepository;
    private final BrandRepository brandRepository;
    private final SellerService sellerService;
    private final RestTemplate restTemplate;

    /**
     * Fetches all products from Shopify and ingests them for the given seller.
     * Row-level errors are captured without failing the entire batch.
     */
    public ShopifyIngestionResultDto ingestProducts(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + sellerId));

        Category sellerCategory = seller.getCategory();
        if (sellerCategory == null) {
            throw new IllegalStateException(
                    "Seller " + sellerId + " has no category assigned — cannot ingest products.");
        }

        ShopifyProductsResponse response = fetchFromShopify();
        List<ShopifyProduct> shopifyProducts = response.getProducts();

        if (shopifyProducts == null || shopifyProducts.isEmpty()) {
            return new ShopifyIngestionResultDto(0, 0, 0, 0, 0, List.of());
        }

        int inserted = 0, updated = 0, skipped = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (ShopifyProduct sp : shopifyProducts) {
            try {
                IngestionOutcome outcome = ingestSingleProduct(sp, seller, sellerCategory);
                switch (outcome) {
                    case INSERTED -> inserted++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }
            } catch (Exception e) {
                failed++;
                String msg = "Failed to ingest Shopify product id=" + sp.getId()
                        + " title=\"" + sp.getTitle() + "\": " + e.getMessage();
                log.error(msg, e);
                errors.add(msg);
            }
        }

        return new ShopifyIngestionResultDto(
                shopifyProducts.size(), inserted, updated, skipped, failed, errors);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private enum IngestionOutcome {
        INSERTED, UPDATED, SKIPPED
    }

    @Transactional
    private IngestionOutcome ingestSingleProduct(ShopifyProduct sp, Seller seller, Category category) {
        String externalId = String.valueOf(sp.getId());
        ZonedDateTime externalUpdatedAt = parseDate(sp.getUpdatedAt());

        // Idempotency check: skip if we already have this exact version
        if (standardProductRepository.existsByExternalIdAndExternalUpdatedAt(externalId, externalUpdatedAt)) {
            log.debug("Skipping up-to-date product externalId={}", externalId);
            return IngestionOutcome.SKIPPED;
        }

        Optional<StandardProduct> existingStd = standardProductRepository.findByExternalId(externalId);
        boolean isNew = existingStd.isEmpty();

        Brand brand = resolveOrCreateBrand(sp.getVendor(), category);

        StandardProduct stdProduct = existingStd.orElseGet(StandardProduct::new);
        populateStandardProduct(stdProduct, sp, category, brand, externalId, externalUpdatedAt);
        stdProduct = standardProductRepository.save(stdProduct);

        // Find or create the seller's Product that points to this StandardProduct
        Optional<Product> existingProduct = productRepository
                .findBySellerIdAndStandardProductId(seller.getId(), stdProduct.getId());
        boolean isNewProduct = existingProduct.isEmpty();

        Product product = existingProduct.orElseGet(Product::new);
        populateProduct(product, sp, seller, category, stdProduct, isNewProduct);
        product = productRepository.save(product);

        // Rebuild child collections on update (orphanRemoval handles deletes)
        if (!isNewProduct) {
            product.getVariants().clear();
            product.getProductAttributes().clear();
            product.getMedia().clear();
            productRepository.save(product); // flush orphan deletes before re-adding
        }

        // Tags
        Set<Tag> tags = resolveOrCreateTags(sp.getTags());
        product.setTags(tags);

        // Options → CategoryAttributes + ProductAttributes
        List<ShopifyOption> options = sp.getOptions() != null ? sp.getOptions() : List.of();
        Map<Integer, CategoryAttribute> positionToCatAttr = new HashMap<>();
        for (ShopifyOption option : options) {
            CategoryAttribute catAttr = resolveOrCreateCategoryAttribute(option.getName(), category, product);
            if (option.getPosition() != null)
                positionToCatAttr.put(option.getPosition(), catAttr);
            createProductAttributesForOption(product, catAttr, option.getValues());
        }

        // Images → ProductMedia
        List<ShopifyImage> images = sp.getImages() != null ? sp.getImages() : List.of();
        buildProductMedia(product, images);

        // Variants → ProductVariants
        List<ShopifyVariant> variants = sp.getVariants() != null ? sp.getVariants() : List.of();
        buildProductVariants(product, variants, options);

        productRepository.save(product);

        // Propagate primary image URL to StandardProduct
        if (!images.isEmpty() && stdProduct.getPrimaryImageUrl() == null) {
            stdProduct.setPrimaryImageUrl(images.get(0).getSrc());
            standardProductRepository.save(stdProduct);
        }

        // Make the product live: refreshes snapshot, publishes live event, indexes to ES
        sellerService.MakeProductLive(product.getId());

        return isNew ? IngestionOutcome.INSERTED : IngestionOutcome.UPDATED;
    }

    private void populateStandardProduct(StandardProduct std, ShopifyProduct sp, Category category,
            Brand brand, String externalId, ZonedDateTime externalUpdatedAt) {
        std.setExternalId(externalId);
        std.setName(sp.getTitle());
        std.setDescription(stripHtml(sp.getBodyHtml()));
        std.setCategory(category);
        std.setBrandEntity(brand);
        std.setStatus(StandardProduct.Status.ACTIVE);
        std.setIsVerified(false);
        std.setDraftStep(StandardProduct.DraftStep.LIVE);
        std.setExternalUpdatedAt(externalUpdatedAt);
        std.setExternalCreatedAt(parseDate(sp.getCreatedAt()));
        std.setExternalPublishedAt(parseDate(sp.getPublishedAt()));
    }

    /**
     * Finds an existing Brand by normalised vendor name + category, or creates one.
     * New brands are created with approved=false so admin review is still required.
     */
    private Brand resolveOrCreateBrand(String vendorName, Category category) {
        String name = (vendorName != null && !vendorName.isBlank()) ? vendorName.trim() : "Unknown";
        String normalised = name.toLowerCase();
        return brandRepository
                .findByNormalisedNameAndCategoryId(normalised, category.getId())
                .orElseGet(() -> {
                    Brand b = new Brand();
                    b.setName(name);
                    b.setNormalisedName(normalised);
                    b.setCategoryId(category.getId());
                    b.setApproved(false);
                    b.setActive(true);
                    return brandRepository.save(b);
                });
    }

    private void populateProduct(Product product, ShopifyProduct sp, Seller seller,
            Category category, StandardProduct stdProduct, boolean isNew) {
        product.setName(sp.getTitle());
        product.setDescription(stripHtml(sp.getBodyHtml()));
        product.setSeller(seller);
        product.setCategory(category);
        product.setStandardProduct(stdProduct);
        product.setIsStandard(true);
        product.setIsActive(true);
        // CATALOG_SELECTED so MakeProductLive's guard passes and it handles
        // snapshot refresh + ES indexing + event publishing.
        product.setStep(Product.Step.CATALOG_SELECTED);
        if (isNew) {
            product.setSearchIntentCreated(false);
            product.setAverageRating(0.0);
            product.setRatingCount(0);
        }
    }

    private Set<Tag> resolveOrCreateTags(List<String> tagNames) {
        Set<Tag> result = new HashSet<>();
        if (tagNames == null || tagNames.isEmpty())
            return result;
        for (String name : tagNames) {
            if (name == null || name.isBlank())
                continue;
            Tag tag = tagRepository.findByNameIgnoreCase(name.trim())
                    .orElseGet(() -> {
                        Tag t = new Tag();
                        t.setName(name.trim());
                        return tagRepository.save(t);
                    });
            result.add(tag);
        }
        return result;
    }

    /**
     * Finds or creates a CategoryAttribute for the given option name and marks it
     * as
     * a variant attribute so the UI knows to show it as a variant selector.
     */
    private CategoryAttribute resolveOrCreateCategoryAttribute(String optionName, Category category, Product product) {
        return categoryAttributeRepository
                .findByCategoryIdAndName(category.getId(), optionName)
                .orElseGet(() -> {
                    CategoryAttribute ca = new CategoryAttribute();
                    ca.setName(optionName);
                    ca.setCategory(category);
                    ca.setIsVariantAttribute(true);
                    ca.setIs_Required(false);
                    ca.setIsImageAttribute(false);
                    ca.setIsAdditionalAttribute(false);
                    return categoryAttributeRepository.save(ca);
                });
    }

    /**
     * Creates one ProductAttribute row per option value (e.g. "1 Kg", "2 Kg", "3
     * Kg").
     */
    private void createProductAttributesForOption(Product product, CategoryAttribute catAttr, List<String> values) {
        if (values == null)
            return;
        for (String value : values) {
            ProductAttribute pa = new ProductAttribute();
            pa.setCategoryAttribute(catAttr);
            pa.setValue(value);
            pa.setProduct(product);
            product.getProductAttributes().add(pa);
        }
    }

    private void buildProductMedia(Product product, List<ShopifyImage> images) {
        for (ShopifyImage img : images) {
            if (img.getSrc() == null)
                continue;
            ProductMedia media = new ProductMedia();
            media.setProduct(product);
            media.setUrl(img.getSrc());
            // publicId is a Cloudinary concept; we store the Shopify image ID as a stable
            // reference
            media.setPublicId("shopify-" + img.getId());
            media.setMediaType(ProductMedia.MediaType.IMAGE);
            int pos = img.getPosition() != null ? img.getPosition() : 0;
            media.setPosition(pos);
            media.setCover(pos == 1);
            product.getMedia().add(media);
        }
    }

    /**
     * Builds ProductVariants. The combination map is built by matching
     * variant.option1/2/3
     * against the options list (which carries the human-readable name).
     */
    private void buildProductVariants(Product product, List<ShopifyVariant> variants, List<ShopifyOption> options) {
        // Pre-index option by position (1-based) → name
        Map<Integer, String> posToName = new HashMap<>();
        if (options != null) {
            for (ShopifyOption opt : options) {
                if (opt.getPosition() != null)
                    posToName.put(opt.getPosition(), opt.getName());
            }
        }

        for (ShopifyVariant sv : variants) {
            ProductVariant pv = new ProductVariant();
            pv.setSku(sv.getSku());
            pv.setLabel(sv.getTitle());
            pv.setPrice(sv.getPrice());
            pv.setMrp(sv.getCompareAtPrice() != null ? sv.getCompareAtPrice() : sv.getPrice());
            pv.setStock(sv.getInventoryQuantity() != null ? sv.getInventoryQuantity() : 0);

            Map<String, String> combination = new HashMap<>();
            if (sv.getOption1() != null && posToName.containsKey(1))
                combination.put(posToName.get(1), sv.getOption1());
            if (sv.getOption2() != null && posToName.containsKey(2))
                combination.put(posToName.get(2), sv.getOption2());
            if (sv.getOption3() != null && posToName.containsKey(3))
                combination.put(posToName.get(3), sv.getOption3());

            pv.setCombination(combination);
            product.getVariants().add(pv);
        }
    }

    private ShopifyProductsResponse fetchFromShopify() {
        // Fetch raw JSON first to avoid RestTemplate's default converter
        // chain choking on unexpected Shopify fields or null-for-primitive values.
        String raw;
        try {
            raw = restTemplate.getForObject(SHOPIFY_PRODUCTS_URL, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch products from Shopify: " + e.getMessage(), e);
        }
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("Empty response from Shopify products endpoint");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            return mapper.readValue(raw, ShopifyProductsResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Shopify products JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Parses ISO-8601 date strings returned by Shopify (e.g.
     * "2026-05-23T12:09:49+05:30").
     */
    private ZonedDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            return ZonedDateTime.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    /** Strips basic HTML tags from Shopify body_html. */
    private String stripHtml(String html) {
        if (html == null)
            return null;
        return html.replaceAll("<[^>]*>", "").trim();
    }
}
// huiu uhiuoihuouio hukiuoihuiuiuhhbhhhjbhhbbbb