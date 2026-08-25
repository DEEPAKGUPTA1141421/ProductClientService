package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.UserInteractionEvent;
import com.ProductClientService.ProductClientService.Model.UserInteractionEventEntity;
import com.ProductClientService.ProductClientService.Repository.UserInteractionEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles user.interaction events and persists them to the partitioned
 * user_interaction_events table. Delivered via Kafka or Redis depending on
 * app.messaging.provider.
 *
 * Group (Kafka mode): "interaction-persist-group" — separate from
 * product-metrics-group so interaction ingestion can be scaled independently
 * from popularity counter updates.
 *
 * Failure strategy: log and continue. Dropping a single view is acceptable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionEventConsumer {

    private final UserInteractionEventRepository repo;
    private final ObjectMapper objectMapper;

    public void handleInteraction(String payload) {
        try {
            UserInteractionEvent ev = objectMapper.readValue(payload, UserInteractionEvent.class);

            UserInteractionEventEntity row = UserInteractionEventEntity.builder()
                    .userId(ev.getUserId())
                    .productId(ev.getProductId())
                    .eventType(ev.getEventType().code())
                    .sessionId(ev.getSessionId())
                    .dwellMs(ev.getDwellMs())
                    .source(ev.getSource())
                    .eventTs(ev.getTs())
                    .build();

            repo.save(row);
            // TODO(phase-2): async ES bulk update on products-v1.popularity_7d
            //                once the dense_vector mapping ships.
        } catch (Exception e) {
            log.warn("Failed to persist user.interaction: {}", e.getMessage());
        }
    }
}
