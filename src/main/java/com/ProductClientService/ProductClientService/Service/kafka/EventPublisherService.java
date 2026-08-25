package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.*;
import com.ProductClientService.ProductClientService.DTO.events.UserInteractionEvent;
import com.ProductClientService.ProductClientService.Service.messaging.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


/**
 * Thin wrapper over EventPublisher (Kafka or Redis, per app.messaging.provider).
 * Immediate serialization/send setup failures are caught so a broker outage does not fail the API call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    public static final String TOPIC_VIEWED     = "product.viewed";
    public static final String TOPIC_CART_ADDED = "product.cart_added";
    public static final String TOPIC_WISHLISTED = "product.wishlisted";
    public static final String TOPIC_ORDER_DONE = "order.completed";
    public static final String TOPIC_ORDER_RET  = "order.returned";
    public static final String TOPIC_PRODUCT_LIVE      = "product.live";
    public static final String TOPIC_REVIEW_SUBMIT_REQUESTED = "review.submit.requested";
    public static final String TOPIC_REVIEW_SUBMITTED         = "review.submitted";
    public static final String TOPIC_REVIEW_HELPFUL           = "review.helpful";
    public static final String TOPIC_USER_INTERACTION         = "user.interaction";
    public static final String TOPIC_SELLER_LIVE              = "seller.live";

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public void publishProductViewed(UUID productId, UUID userId, UUID categoryId) {
        publish(TOPIC_VIEWED, ProductViewedEvent.builder()
                .productId(productId).userId(userId).categoryId(categoryId).build());
    }

    public void publishCartAdded(UUID productId, UUID variantId, UUID userId) {
        publish(TOPIC_CART_ADDED, ProductCartAddedEvent.builder()
                .productId(productId).variantId(variantId).userId(userId).build());
    }

    public void publishWishlistAdd(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId).userId(userId).action("ADD").build());
    }

    public void publishWishlistRemove(UUID productId, UUID userId) {
        publish(TOPIC_WISHLISTED, ProductWishlistedEvent.builder()
                .productId(productId).userId(userId).action("REMOVE").build());
    }

    /**
     * Published when a seller's status transitions to ACTIVE (admin approval).
     * Consumed by ShopIndexerConsumer to index the seller as a shop in shops-v1.
     */
    public void publishSellerLive(UUID sellerId) {
        publish(TOPIC_SELLER_LIVE, SellerLiveEvent.builder().sellerId(sellerId).build());
    }

    /**
     * Published when a product transitions to LIVE status.
     * Consumed by SearchIntentIndexerConsumer to generate and index
     * search intents into Elasticsearch.
     */
    public void publishProductLive(UUID productId) {
        publish(TOPIC_PRODUCT_LIVE, ProductLiveEvent.builder()
                .productId(productId).build());
    }

    public void publishReviewSubmitRequested(ReviewSubmitRequestedEvent event) {
        publish(TOPIC_REVIEW_SUBMIT_REQUESTED, event);
    }

    public void publishReviewSubmitted(UUID reviewId, UUID productId, UUID userId, int rating) {
        publish(TOPIC_REVIEW_SUBMITTED, ReviewSubmittedEvent.builder()
                .reviewId(reviewId).productId(productId).userId(userId).rating(rating).build());
    }

    public void publishReviewHelpfulAdd(UUID reviewId, UUID userId) {
        publish(TOPIC_REVIEW_HELPFUL, ReviewHelpfulEvent.builder()
                .reviewId(reviewId).userId(userId).action("ADD").build());
    }

    public void publishReviewHelpfulRemove(UUID reviewId, UUID userId) {
        publish(TOPIC_REVIEW_HELPFUL, ReviewHelpfulEvent.builder()
                .reviewId(reviewId).userId(userId).action("REMOVE").build());
    }

    /**
     * Publish a batch of user interactions using productId as the partition key
     * so all events for the same product land on the same partition (preserves
     * per-product ordering for downstream CF feature windows).
     */
    public void publishInteractionBatch(java.util.List<UserInteractionEvent> events) {
        if (events == null || events.isEmpty()) return;
        for (UserInteractionEvent ev : events) {
            try {
                String key = ev.getProductId() != null ? ev.getProductId().toString() : null;
                eventPublisher.publish(TOPIC_USER_INTERACTION, key, objectMapper.writeValueAsString(ev));
            } catch (Exception e) {
                log.warn("Failed to publish user.interaction: {}", e.getMessage());
            }
        }
    }

    private void publish(String topic, Object event) {
        try {
            eventPublisher.publish(topic, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("Failed to publish to topic={}: {}", topic, e.getMessage());
        }
    }
}
