package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Hot table — one row per user action. Backs the offline recommendation
 * training pipeline and online drift detection.
 *
 * Physical table is PARTITIONED BY RANGE (event_ts) monthly; see
 * src/main/resources/db/migration/V20__interaction_events.sql.
 * Hibernate's ddl-auto=update will only add missing columns, not recreate
 * a partitioned table, so the migration must run first in any fresh env.
 */
@Entity
@Table(
    name = "user_interaction_events"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "event_type", nullable = false)
    private short eventType;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "dwell_ms")
    private Integer dwellMs;

    @Column(name = "event_ts", nullable = false)
    private Instant eventTs;

    @Column(name = "source", length = 16)
    private String source;
}
