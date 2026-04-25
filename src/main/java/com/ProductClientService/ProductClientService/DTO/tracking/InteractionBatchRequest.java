package com.ProductClientService.ProductClientService.DTO.tracking;

import com.ProductClientService.ProductClientService.DTO.events.InteractionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class InteractionBatchRequest {

    @NotNull
    private UUID sessionId;

    @NotEmpty
    @Size(max = 20, message = "Batch capped at 20 events per request")
    @Valid
    private List<Event> events;

    @Data
    public static class Event {
        @NotNull private UUID productId;
        @NotNull private InteractionType eventType;
        private Integer dwellMs;
        private String source;
        private Instant ts;
    }
}
