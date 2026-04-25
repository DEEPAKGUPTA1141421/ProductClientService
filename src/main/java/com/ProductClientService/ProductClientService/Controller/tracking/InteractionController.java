package com.ProductClientService.ProductClientService.Controller.tracking;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.events.InteractionType;
import com.ProductClientService.ProductClientService.DTO.events.UserInteractionEvent;
import com.ProductClientService.ProductClientService.DTO.tracking.InteractionBatchRequest;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import com.ProductClientService.ProductClientService.Service.session.SessionClickBufferService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Accepts batches of user interaction events from mobile/web clients.
 * Endpoint is public: guests (no JWT) must also be trackable, because
 * anonymous-session → sign-up → purchase is a critical funnel. When a JWT
 * is present we attach userId, otherwise events flow with userId=null and
 * the offline pipeline uses sessionId for CF signal until the user logs in.
 */
@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
@Slf4j
public class InteractionController {

    private final EventPublisherService publisher;
    private final SessionClickBufferService sessionBuffer;

    @PostMapping("/interaction")
    public ResponseEntity<ApiResponse<Object>> trackBatch(@Valid @RequestBody InteractionBatchRequest req) {
        UUID userId = resolveUserIdIfPresent();
        Instant now = Instant.now();

        List<UserInteractionEvent> events = new ArrayList<>(req.getEvents().size());
        for (InteractionBatchRequest.Event e : req.getEvents()) {
            UserInteractionEvent ev = UserInteractionEvent.builder()
                    .userId(userId)
                    .productId(e.getProductId())
                    .eventType(e.getEventType())
                    .sessionId(req.getSessionId())
                    .dwellMs(e.getDwellMs())
                    .source(e.getSource())
                    .ts(e.getTs() != null ? e.getTs() : now)
                    .build();
            events.add(ev);

            if (e.getEventType() == InteractionType.CLICK) {
                sessionBuffer.pushClick(req.getSessionId(), e.getProductId());
            }
        }

        publisher.publishInteractionBatch(events);
        return ResponseEntity.accepted()
                .body(new ApiResponse<>(true, "Accepted", null, 202));
    }

    private UUID resolveUserIdIfPresent() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal up) {
                return up.getId();
            }
        } catch (Exception ignored) { }
        return null;
    }
}
