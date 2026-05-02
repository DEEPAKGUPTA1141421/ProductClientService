package com.ProductClientService.ProductClientService.Controller.internal;

import com.ProductClientService.ProductClientService.DTO.FcmPushRequest;
import com.ProductClientService.ProductClientService.Service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal endpoint for other microservices (booking, delivery) to trigger
 * FCM push notifications without directly depending on Firebase Admin SDK.
 *
 * Protected by @PrivateApi (JWT aspect checks the X-Internal-Secret header).
 */
@RestController
@RequestMapping("/api/v1/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final FcmService fcmService;

    /**
     * Send a push notification to a user.
     * Request body: { userId, title, body, data? }
     */
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody FcmPushRequest req) {
        if (req.getUserId() == null || req.getTitle() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId and title are required"));
        }
        fcmService.sendToUser(req.getUserId(), req.getTitle(), req.getBody(), req.getData());
        return ResponseEntity.ok(Map.of("queued", true));
    }

    /**
     * Convenience endpoint for order status notifications.
     * POST { userId, bookingId, status } — title/body are generated from the status
     * string.
     */
    @PostMapping("/order-status")
    public ResponseEntity<?> orderStatus(@RequestBody Map<String, String> body) {
        String userIdStr = body.get("userId");
        String bookingId = body.get("bookingId");
        String status = body.get("status");

        if (userIdStr == null || bookingId == null || status == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId, bookingId and status are required"));
        }

        fcmService.sendOrderStatusNotification(
                java.util.UUID.fromString(userIdStr),
                bookingId,
                status);

        return ResponseEntity.ok(Map.of("queued", true));
    }
}
