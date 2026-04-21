package com.ProductClientService.ProductClientService.DTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingDTO {
    private UUID id;
    private UUID productId;
    private int rating;
    private String title;
    private String review;
    private List<String> reviewImages;
    private int helpfulCount;
    private boolean verifiedPurchase;
    private ZonedDateTime createdAt;
    private UserSummaryDTO user;

    public static ProductRatingDTO fromEntity(ProductRating r) {
        User user = r.getUser();
        return new ProductRatingDTO(
                r.getId(),
                r.getProduct().getId(),
                r.getRating(),
                r.getTitle(),
                r.getReview(),
                r.getReviewImages(),
                r.getHelpfulCount(),
                r.isVerifiedPurchase(),
                r.getCreatedAt(),
                UserSummaryDTO.fromEntity(user));
    }
}
