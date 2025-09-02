package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String description;

    // IMAGE / VIDEO
    @Enumerated(EnumType.ORDINAL)
    private MediaType mediaType;

    private String mediaUrl; // link to image or video in CDN

    private String logoUrl; // optional

    // PRODUCT / PRODUCT_LIST / MARKETING
    @Enumerated(EnumType.ORDINAL)
    private BannerType bannerType;

    // What happens when user clicks
    @Enumerated(EnumType.ORDINAL)
    private RedirectType redirectType;

    private String redirectRefId;
    // e.g. productId if PRODUCT, categoryId if PRODUCT_LIST, marketingPageId if
    // MARKETING

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
    // Optional JSON for extra info (filters, tags, etc.)

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean isActive = true;

    private Integer priority; // control order of display
    private String category;

    public enum MediaType {
        IMAGE,
        VIDEO
    }

    public enum BannerType {
        PRODUCT,
        PRODUCT_LIST,
        MARKETING
    }

    public enum RedirectType {
        PRODUCT, // open single product
        CATEGORY, // open list of products
        MARKETING // open marketing page
    }
}
