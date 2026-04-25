package com.ProductClientService.ProductClientService.tracking;

import com.ProductClientService.ProductClientService.DTO.events.InteractionType;
import com.ProductClientService.ProductClientService.DTO.events.UserInteractionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserInteractionEventSerdeTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void roundTripsThroughJson() throws Exception {
        UserInteractionEvent ev = UserInteractionEvent.builder()
                .userId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .eventType(InteractionType.PURCHASE_COD)
                .sessionId(UUID.randomUUID())
                .dwellMs(2400)
                .source("pdp")
                .ts(Instant.parse("2026-04-22T10:12:04Z"))
                .build();

        String json = mapper.writeValueAsString(ev);
        UserInteractionEvent back = mapper.readValue(json, UserInteractionEvent.class);

        assertEquals(ev, back);
        assertEquals(InteractionType.PURCHASE_COD, back.getEventType());
        assertEquals(14.0, back.getEventType().weight(), 0.0001,
                "COD weight must remain highest (stronger signal than prepaid in Tier-2/3)");
    }

    @Test
    void codeMappingIsStable() {
        // Codes are persisted as SMALLINT in Postgres — changing them silently
        // would corrupt all historical training data. Lock them here.
        assertEquals(1, InteractionType.VIEW.code());
        assertEquals(2, InteractionType.CLICK.code());
        assertEquals(3, InteractionType.WISHLIST.code());
        assertEquals(4, InteractionType.CART.code());
        assertEquals(5, InteractionType.PURCHASE_PREPAID.code());
        assertEquals(6, InteractionType.PURCHASE_COD.code());
    }

    @Test
    void codPurchaseWeightsHigherThanPrepaid() {
        assertTrue(InteractionType.PURCHASE_COD.weight()
                > InteractionType.PURCHASE_PREPAID.weight());
    }
}
