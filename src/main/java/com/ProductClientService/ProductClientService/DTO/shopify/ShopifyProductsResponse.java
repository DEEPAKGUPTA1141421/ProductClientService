package com.ProductClientService.ProductClientService.DTO.shopify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopifyProductsResponse {

    @JsonProperty("products")
    private List<ShopifyProduct> products;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyProduct {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("body_html")
        private String bodyHtml;

        @JsonProperty("published_at")
        private String publishedAt;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        // Brand / store name on Shopify
        @JsonProperty("vendor")
        private String vendor;

        // Shopify returns tags as a JSON array e.g. ["organic", "gluten-free"]
        @JsonProperty("tags")
        private List<String> tags;

        @JsonProperty("options")
        private List<ShopifyOption> options;

        @JsonProperty("images")
        private List<ShopifyImage> images;

        @JsonProperty("variants")
        private List<ShopifyVariant> variants;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyOption {

        @JsonProperty("name")
        private String name;

        @JsonProperty("position")
        private Integer position;

        @JsonProperty("values")
        private List<String> values;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyImage {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("position")
        private Integer position;

        @JsonProperty("src")
        private String src;

        @JsonProperty("variant_ids")
        private List<Long> variantIds;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyVariant {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("price")
        private String price;

        @JsonProperty("compare_at_price")
        private String compareAtPrice;

        @JsonProperty("sku")
        private String sku;

        @JsonProperty("option1")
        private String option1;

        @JsonProperty("option2")
        private String option2;

        @JsonProperty("option3")
        private String option3;

        // Nullable when inventory tracking is disabled on Shopify
        @JsonProperty("inventory_quantity")
        private Integer inventoryQuantity;
    }
}
