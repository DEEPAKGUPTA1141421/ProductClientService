package com.ProductClientService.ProductClientService.Model;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "search_intents", uniqueConstraints = @UniqueConstraint(columnNames = "keyword"))
@Getter
@Setter
public class SearchIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false, length = 2000)
    private String imageUrl;

    private String suggestionType;
    // CATEGORY, BRAND_CATEGORY, ATTRIBUTE_CATEGORY, TAG_CATEGORY

    private UUID referenceId;

    private Long searchCount = 0L;

    private Long clickCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
}
