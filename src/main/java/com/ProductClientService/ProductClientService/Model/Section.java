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

    @Enumerated(EnumType.ORDINAL)
    private SectionType type; // PRODUCT_GRID, PRODUCT_SCROLL, PRODUCT_HIGHLIGHT, BANNER

    @JdbcTypeCode(SqlTypes.JSON) // ✅ Hibernate 6 built-in JSON
    @Column(columnDefinition = "jsonb")
    private JsonNode config;

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
}
// lniuhiuhb hgugybgfuttguygug jvuygugbvyttfytfy