package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "section_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @JsonIgnore
    private Section section;

    @Enumerated(EnumType.ORDINAL)
    private ItemType itemType; // PRODUCT, BANNER, MARKETING_PAGE

    private String itemRefId; // Reference ID (Product ID, Banner ID, Marketing Page ID, etc.)

    @JdbcTypeCode(SqlTypes.JSON) // ✅ Hibernate 6 built-in JSON
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    public enum ItemType {
        PRODUCT,
        BANNER,
        MARKETING_PAGE,
        SPONSORED_PRODUCT,
        SPONSORED_BRAND,
        BRAND,
        CATEGORY,
    }
}

// gyuguy hggjt hjgytty ghfytf bhjbgjyunm jhgygm nkhiuhjhbh
