package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * FcmService
 * ───────────
 * Sends Firebase Cloud Messaging push notifications to registered devices.
 *
 * Initialisation: reads service account JSON at startup (path configured via
 * FCM_SERVICE_ACCOUNT_PATH env var, defaults to "firebase-service-account.json").
 *
 * Graceful degradation: if the service account file is missing or invalid,
 * all send() calls are silently no-ops — the rest of the app is unaffected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final UserRepojectory userRepository;

    @Value("${fcm.service-account-path:firebase-service-account.json}")
    private String serviceAccountPath;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                initialized = true;
                return;
            }
            FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("Firebase Admin SDK initialized");
        } catch (Exception e) {
            log.warn("Firebase Admin SDK init skipped — push notifications disabled: {}", e.getMessage());
        }
    }

    // ── Low-level send ────────────────────────────────────────────────────────

    @Async
    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        if (!initialized || fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null) builder.putAllData(data);
            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.debug("FCM sent msgId={} token={}…", response, fcmToken.substring(0, Math.min(12, fcmToken.length())));
        } catch (Exception e) {
            log.warn("FCM send failed: {}", e.getMessage());
        }
    }

    // ── User-targeted send ────────────────────────────────────────────────────

    @Async
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        Optional<User> opt = userRepository.findById(userId);
        if (opt.isEmpty()) return;
        String token = opt.get().getFcmToken();
        if (token == null || token.isBlank()) return;
        send(token, title, body, data);
    }

    // ── Order status helper ───────────────────────────────────────────────────

    @Async
    public void sendOrderStatusNotification(UUID userId, String bookingId, String status) {
        String title = orderTitle(status);
        String body  = orderBody(status);
        sendToUser(userId, title, body, Map.of(
                "type",      "ORDER_STATUS",
                "bookingId", bookingId,
                "status",    status
        ));
    }

    private String orderTitle(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED"  -> "Order Confirmed";
            case "SHIPPED"    -> "Order Shipped";
            case "DELIVERED"  -> "Order Delivered!";
            case "CANCELLED"  -> "Order Cancelled";
            default           -> "Order Update";
        };
    }

    private String orderBody(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED"  -> "Your order has been confirmed by the seller.";
            case "SHIPPED"    -> "Your order is on its way!";
            case "DELIVERED"  -> "Your order has been delivered. Enjoy!";
            case "CANCELLED"  -> "Your order has been cancelled.";
            default           -> "Your order status has been updated.";
        };
    }
}
