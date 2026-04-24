package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Materialised nightly by AffinitySnapshotJob. Feeds the reranker + the
 * cold-start resolver when the user has some history but the online model
 * has not refreshed user vectors yet.
 */
@Entity
@Table(name = "user_affinity_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAffinitySnapshot {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    /** [{ "catId": "...", "score": 0.42 }, ...] — top 10 by weighted score. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_categories", columnDefinition = "jsonb")
    private Object topCategories;

    @Column(name = "price_band_low", precision = 12, scale = 2)
    private BigDecimal priceBandLow;

    @Column(name = "price_band_high", precision = 12, scale = 2)
    private BigDecimal priceBandHigh;

    @Column(name = "lang_pref", length = 8)
    private String langPref;

    @Column(name = "cod_ratio", precision = 3, scale = 2)
    private BigDecimal codRatio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
