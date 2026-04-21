package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

    Optional<ReviewLike> findByReviewIdAndUserId(UUID reviewId, UUID userId);

    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

    void deleteByReviewIdAndUserId(UUID reviewId, UUID userId);
}
