package com.ProductClientService.ProductClientService.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Tracks which users marked a review as "helpful".
 * The unique constraint on (review_id, user_id) enforces one vote per user per review.
 */
@Entity
@Table(name = "review_likes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    public ReviewLike(UUID reviewId, UUID userId) {
        this.reviewId = reviewId;
        this.userId = userId;
    }
}
