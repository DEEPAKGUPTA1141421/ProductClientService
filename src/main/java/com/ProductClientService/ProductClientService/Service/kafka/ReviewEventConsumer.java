package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.ReviewHelpfulEvent;
import com.ProductClientService.ProductClientService.DTO.events.ReviewSubmittedEvent;
import com.ProductClientService.ProductClientService.Service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Processes review-related Kafka events.
 *
 * review.submitted — triggers async product avg-rating recalculation.
 * review.helpful   — increments/decrements helpful_count in Postgres.
 *
 * Failure strategy: log and ack (same as metrics consumers — a missed
 * increment is acceptable; stalling the partition is not).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "review.submitted", groupId = "review-events-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onReviewSubmitted(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ReviewSubmittedEvent event = objectMapper.readValue(record.value(), ReviewSubmittedEvent.class);
            // Re-compute and persist the product's cached avg_rating + rating_count
            reviewService.updateProductRatingSummaryAsync(event.getProductId());
            log.debug("Processed review.submitted reviewId={} productId={}",
                    event.getReviewId(), event.getProductId());
        } catch (Exception e) {
            log.warn("Failed to process review.submitted: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "review.helpful", groupId = "review-events-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onReviewHelpful(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ReviewHelpfulEvent event = objectMapper.readValue(record.value(), ReviewHelpfulEvent.class);
            log.debug("Processed review.helpful reviewId={} action={}", event.getReviewId(), event.getAction());
            // helpful_count is already updated in ReviewService.toggleHelpful — this topic
            // exists for future downstream consumers (analytics, notifications, etc.).
        } catch (Exception e) {
            log.warn("Failed to process review.helpful: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }
}
