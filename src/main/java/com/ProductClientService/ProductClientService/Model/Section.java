package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title; // e.g. "Deals For You", "Sponsored"

    @Enumerated(EnumType.STRING)
    private SectionType type; // PRODUCT_GRID, PRODUCT_SCROLL, PRODUCT_HIGHLIGHT, BANNER

    private String widgetKey; // e.g. "product_grid_v1", "banner_hero_v1"

    @Enumerated(EnumType.STRING)
    private DataSource dataSource; // STATIC, RECO_FOR_YOU, RECO_TRENDING, CATEGORY_TOP, SEARCH_QUERY

    @JdbcTypeCode(SqlTypes.JSON) // ✅ Hibernate 6 built-in JSON
    @Column(columnDefinition = "jsonb")
    private JsonNode config;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode dataParams; // e.g. {"recoContext":"HOME","k":20} for RECO sections

    private java.time.OffsetDateTime startsAt; // campaign scheduling
    private java.time.OffsetDateTime endsAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode audience; // e.g. {"cities":["BLR"],"authed":true}

    private int version; // for rollback / A/B testing

    private int position; // order on page
    // --- Add OneToMany relationship ---
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SectionItem> items = new ArrayList<>();
    private String category; // e.g. "For You", "Mobile", "Electronics"

    private boolean active;

    public enum SectionType {
        PRODUCT_GRID,
        PRODUCT_SCROLL,
        PRODUCT_HIGHLIGHT,
        BANNER,
        SPONSORED,
        BRAND,
        CATEGORY,
    }

    public enum DataSource {
        STATIC,             // items from section_items table
        RECO_FOR_YOU,       // personalized reco orchestrator
        RECO_TRENDING,      // trending/popular cold-start
        CATEGORY_TOP,       // top products in a category
        SEARCH_QUERY        // search results
    }
}
// lniuhiuhb hgugybgfuttguygug jvuygugbvyttfytfy