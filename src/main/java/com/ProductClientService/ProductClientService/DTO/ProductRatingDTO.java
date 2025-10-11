package com.ProductClientService.ProductClientService.DTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

import com.ProductClientService.ProductClientService.Model.ProductRating;
import com.ProductClientService.ProductClientService.Model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingDTO {
    private UUID id;
    private UUID productId;
    private int rating;
    private String review;
    private ZonedDateTime createdAt;

    // Nested user details
    private UserSummaryDTO user;

    public static ProductRatingDTO fromEntity(ProductRating rating) {
        User user = rating.getUser();
        return new ProductRatingDTO(
                rating.getId(),
                rating.getProduct().getId(),
                rating.getRating(),
                rating.getReview(),
                rating.getCreatedAt(),
                UserSummaryDTO.fromEntity(user));
    }
}
