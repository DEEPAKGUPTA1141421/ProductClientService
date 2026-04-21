package com.ProductClientService.ProductClientService.Service.kafka;

import com.ProductClientService.ProductClientService.DTO.events.ReviewHelpfulEvent;
import com.ProductClientService.ProductClientService.DTO.events.ReviewSubmitRequestedEvent;
import com.ProductClientService.ProductClientService.Model.Product;
import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.ImageUploadService;
import com.ProductClientService.ProductClientService.Service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processes review pipeline events off the HTTP request thread.
 *
 * review.submit.requested
 *   1. Upload each image to Cloudinary
 *   2. Save / update ProductRating in Postgres
 *   3. Sync avg_rating + rating_count on Product
 *
 * review.helpful
 *   Placeholder for future downstream consumers (analytics, notifications).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final ObjectMapper objectMapper;
    private final ImageUploadService imageUploadService;
    private final ProductRatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final UserRepojectory userRepository;
    private final ReviewService reviewService;

    @KafkaListener(topics = "review.submit.requested", groupId = "review-events-group",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onReviewSubmitRequested(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ReviewSubmitRequestedEvent event =
                    objectMapper.readValue(record.value(), ReviewSubmitRequestedEvent.class);

            // ── 1. Upload images to Cloudinary ────────────────────────────────
            List<String> imageUrls = uploadImages(event.getImageBytes(), event.getUserId());

            // ── 2. Save / update ProductRating in Postgres ────────────────────
            UUID productId = event.getProductId();
            UUID userId    = event.getUserId();

            Optional<ProductRating> existing =
                    ratingRepository.findByProductIdAndUserId(productId, userId);

            ProductRating review;
            if (existing.isPresent()) {
                review = existing.get();
                review.setRating(event.getRating());
                review.setTitle(event.getTitle());
                review.setReview(event.getReviewText());
                if (!imageUrls.isEmpty()) {
                    review.getReviewImages().clear();
                    review.getReviewImages().addAll(imageUrls);
                }
            } else {
                User userRef = new User();
                userRef.setId(userId);

                Product productRef = new Product();
                productRef.setId(productId);

                review = new ProductRating();
                review.setProduct(productRef);
                review.setUser(userRef);
                review.setRating(event.getRating());
                review.setTitle(event.getTitle());
                review.setReview(event.getReviewText());
                review.getReviewImages().addAll(imageUrls);
                review.setVerifiedPurchase(true);
            }

            ProductRating saved = ratingRepository.save(review);

            // ── 3. Sync cached avg_rating + rating_count on Product ───────────
            reviewService.updateProductRatingSummaryAsync(productId);

            log.info("Review saved reviewId={} productId={}", saved.getId(), productId);
        } catch (Exception e) {
            log.error("Failed to process review.submit.requested: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "review.helpful", groupId = "review-events-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onReviewHelpful(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ReviewHelpfulEvent event = objectMapper.readValue(record.value(), ReviewHelpfulEvent.class);
            log.debug("review.helpful reviewId={} action={}", event.getReviewId(), event.getAction());
        } catch (Exception e) {
            log.warn("Failed to process review.helpful: {}", e.getMessage());
        } finally {
            ack.acknowledge();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> uploadImages(List<byte[]> imageBytesList, UUID userId) {
        List<String> urls = new ArrayList<>();
        if (imageBytesList == null || imageBytesList.isEmpty()) return urls;

        for (byte[] bytes : imageBytesList) {
            try {
                urls.add(imageUploadService.uploadImage(bytes));
            } catch (Exception e) {
                log.warn("Image upload failed for userId={}: {}", userId, e.getMessage());
            }
        }
        return urls;
    }
}
