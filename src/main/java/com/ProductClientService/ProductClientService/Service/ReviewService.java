package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductRatingDTO;
import com.ProductClientService.ProductClientService.DTO.RatingSummaryDTO;
import com.ProductClientService.ProductClientService.DTO.events.ReviewSubmitRequestedEvent;
import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.ReviewLike;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.ProductRatingRepository;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Repository.ReviewLikeRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService extends BaseService {

    private final ProductRatingRepository ratingRepository;
    private final ReviewLikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final UserRepojectory userRepository;
    private final EventPublisherService eventPublisher;

    // ── Submit / update review ────────────────────────────────────────────────

    public ApiResponse<Object> submitReview(
            UUID productId,
            int rating,
            String title,
            String reviewText,
            List<MultipartFile> images) {

        // ── Fast validations only — user must not wait for anything else ──────
        if (rating < 1 || rating > 5) {
            return new ApiResponse<>(false, "Rating must be between 1 and 5", null, 400);
        }

        UUID userId = getUserId();

        if (!productRepository.existsById(productId)) {
            return new ApiResponse<>(false, "Product not found", null, 404);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPurchasedProductIds().contains(productId)) {
            return new ApiResponse<>(
                    false,
                    "Review not allowed. You must purchase the product before submitting a review.",
                    null,
                    422);
        }

        // ── Read image bytes from request scope before HTTP response is sent ──
        // MultipartFile data lives only in the request — must be captured here.
        List<byte[]> imageBytes = new ArrayList<>();
        if (images != null) {
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    try {
                        imageBytes.add(file.getBytes());
                        if (imageBytes.size() == 5)
                            break;
                    } catch (Exception e) {
                        log.warn("Could not read image bytes for userId={}: {}", userId, e.getMessage());
                    }
                }
            }
        }

        // ── Publish to Kafka and return immediately (202) ─────────────────────
        // Consumer handles: Cloudinary upload → DB save → avg-rating sync
        eventPublisher.publishReviewSubmitRequested(
                ReviewSubmitRequestedEvent.builder()
                        .productId(productId)
                        .userId(userId)
                        .rating(rating)
                        .title(sanitize(title))
                        .reviewText(sanitize(reviewText))
                        .imageBytes(imageBytes)
                        .build());

        return new ApiResponse<>(true, "Review is being processed", null, 202);
    }

    // ── Get paginated reviews ─────────────────────────────────────────────────

    public ApiResponse<Object> getReviews(UUID productId, int page, int size, String sort) {
        Sort sortSpec = switch (sort) {
            case "helpful" -> Sort.by(Sort.Direction.DESC, "helpfulCount");
            case "highest" -> Sort.by(Sort.Direction.DESC, "rating");
            case "lowest" -> Sort.by(Sort.Direction.ASC, "rating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 50), sortSpec);
        Page<ProductRating> pageResult = ratingRepository.findByProductId(productId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", pageResult.getContent().stream()
                .map(ProductRatingDTO::fromEntity).toList());
        response.put("totalElements", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("page", page);
        response.put("hasMore", pageResult.hasNext());

        return new ApiResponse<>(true, "Reviews fetched", response, 200);
    }

    // ── Rating summary (distribution histogram) ───────────────────────────────

    public ApiResponse<Object> getRatingSummary(UUID productId) {
        List<Object[]> distRows = ratingRepository.findStarDistributionByProductId(productId);
        Double avg = ratingRepository.findAverageRatingByProductId(productId);
        Long total = ratingRepository.countRatingsByProductId(productId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--)
            distribution.put(i, 0L);
        for (Object[] row : distRows) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(star, count);
        }

        RatingSummaryDTO summary = RatingSummaryDTO.builder()
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .totalRatings(total != null ? total : 0L)
                .distribution(distribution)
                .build();

        return new ApiResponse<>(true, "Rating summary fetched", summary, 200);
    }

    // ── Mark review helpful (toggle) ──────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> toggleHelpful(UUID reviewId) {
        UUID userId = getUserId();

        if (!ratingRepository.existsById(reviewId)) {
            return new ApiResponse<>(false, "Review not found", null, 404);
        }

        if (likeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            likeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            ratingRepository.decrementHelpfulCount(reviewId);
            eventPublisher.publishReviewHelpfulRemove(reviewId, userId);
            return new ApiResponse<>(true, "Removed helpful vote", Map.of("action", "REMOVED"), 200);
        } else {
            likeRepository.save(new ReviewLike(reviewId, userId));
            ratingRepository.incrementHelpfulCount(reviewId);
            eventPublisher.publishReviewHelpfulAdd(reviewId, userId);
            return new ApiResponse<>(true, "Marked as helpful", Map.of("action", "ADDED"), 200);
        }
    }

    // ── Delete own review ─────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Object> deleteReview(UUID reviewId) {
        UUID userId = getUserId();

        ProductRating review = ratingRepository.findById(reviewId)
                .orElse(null);

        if (review == null) {
            return new ApiResponse<>(false, "Review not found", null, 404);
        }

        if (!review.getUser().getId().equals(userId)) {
            return new ApiResponse<>(false, "Not authorized to delete this review", null, 403);
        }

        UUID productId = review.getProduct().getId();
        likeRepository.deleteAll(likeRepository.findAll().stream()
                .filter(l -> l.getReviewId().equals(reviewId)).toList());
        ratingRepository.delete(review);
        updateProductRatingSummaryAsync(productId);

        return new ApiResponse<>(true, "Review deleted", null, 200);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void updateProductRatingSummaryAsync(UUID productId) {
        try {
            List<Object[]> results = ratingRepository.findAvgAndCountByProductId(productId);
            double avg = 0.0;
            int count = 0;
            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                avg = row[0] != null ? ((Number) row[0]).doubleValue() : 0.0;
                count = row[1] != null ? ((Number) row[1]).intValue() : 0;
            }
            productRepository.updateProductRating(productId, avg, count);
        } catch (Exception e) {
            log.error("Failed to update product rating summary for productId={}: {}", productId, e.getMessage());
        }
    }

    /** Strip HTML tags and trim to prevent XSS stored in review text. */
    private String sanitize(String input) {
        if (input == null)
            return null;
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
// dnjfj NJJ djkhjiuhuiuj