package com.ProductClientService.ProductClientService.DTO.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to topic: user.interaction
 * Consumed by InteractionEventConsumer → persisted in Postgres partitioned
 * table + asynchronously bumps popularity_7d in Elasticsearch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionEvent {
    private UUID userId;            // null for guest
    private UUID productId;
    private InteractionType eventType;
    private UUID sessionId;
    private Integer dwellMs;        // nullable
    private String source;          // home | search | pdp | push | cart
    @Builder.Default
    private Instant ts = Instant.now();
}
