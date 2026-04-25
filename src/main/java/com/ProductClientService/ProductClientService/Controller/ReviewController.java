package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.ReviewService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Review endpoints:
 *
 *  POST   /api/v1/reviews/{productId}             — submit/update own review  [AUTH]
 *  GET    /api/v1/reviews/{productId}             — paginated review list      [PUBLIC]
 *  GET    /api/v1/reviews/{productId}/summary     — star distribution summary  [PUBLIC]
 *  POST   /api/v1/reviews/{reviewId}/helpful      — toggle helpful vote        [AUTH]
 *  DELETE /api/v1/reviews/{reviewId}              — delete own review          [AUTH]
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** Submit or update the authenticated user's review for a product. */
    @PostMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitReview(
            @PathVariable UUID productId,
            @RequestParam @Min(1) @Max(5) int rating,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String review,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApiResponse<Object> response = reviewService.submitReview(productId, rating, title, review, images);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Paginated review list — public, no auth required. */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {

        ApiResponse<Object> response = reviewService.getReviews(productId, page, size, sort);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Star distribution summary — public, no auth required. */
    @GetMapping("/{productId}/summary")
    public ResponseEntity<?> getRatingSummary(@PathVariable UUID productId) {
        ApiResponse<Object> response = reviewService.getRatingSummary(productId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Toggle helpful vote on a review (add if not voted, remove if already voted). */
    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<?> toggleHelpful(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApiResponse<Object> response = reviewService.toggleHelpful(reviewId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    /** Delete the authenticated user's own review. */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApiResponse<Object> response = reviewService.deleteReview(reviewId);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
