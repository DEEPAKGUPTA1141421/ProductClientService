package com.ProductClientService.ProductClientService.DTO;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * Request body for the internal push-notification endpoint.
 * Called by other services (booking, delivery) to trigger user-targeted FCM pushes.
 */
@Data
public class FcmPushRequest {
    /** Target user — looked up in users table for their fcmToken. */
    private UUID userId;
    private String title;
    private String body;
    /** Optional key-value data payload (e.g. bookingId, status). */
    private Map<String, String> data;
}
