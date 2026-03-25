package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ProductClientService.ProductClientService.Model.SearchIntent;

@Repository
public interface SearchIntentRepository extends JpaRepository<SearchIntent, UUID> {
    boolean existsByKeyword(String keyword);

    Optional<SearchIntent> findByKeyword(String keyword);

    /**
     * Full-text prefix search for autocomplete.
     * Returns top 10 matching suggestions ordered by click count desc.
     */
    @Query(value = """
            SELECT * FROM search_intents
            WHERE keyword ILIKE CONCAT(:prefix, '%')
            ORDER BY click_count DESC, search_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<SearchIntent> findTopByKeywordStartingWith(@Param("prefix") String prefix);

    /**
     * Fuzzy search for autocomplete (handles typos).
     */
    @Query(value = """
            SELECT * FROM search_intents
            WHERE
                (LENGTH(:keyword) < 3 AND keyword ILIKE CONCAT(:keyword, '%'))
                OR
                (LENGTH(:keyword) >= 3 AND (
                    keyword ILIKE CONCAT('%', :keyword, '%')
                    OR keyword % :keyword
                ))
            ORDER BY
                CASE WHEN keyword ILIKE CONCAT(:keyword, '%') THEN 1
                     WHEN keyword ILIKE CONCAT('%', :keyword, '%') THEN 2
                     ELSE 3 END,
                click_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<SearchIntent> fuzzySearch(@Param("keyword") String keyword);

    /** Increment search count atomically when user searches this keyword. */
    @Modifying
    @Transactional
    @Query("UPDATE SearchIntent s SET s.searchCount = s.searchCount + 1 WHERE s.id = :id")
    void incrementSearchCount(@Param("id") UUID id);

    /** Increment click count atomically when user clicks a suggestion. */
    @Modifying
    @Transactional
    @Query("UPDATE SearchIntent s SET s.clickCount = s.clickCount + 1 WHERE s.id = :id")
    void incrementClickCount(@Param("id") UUID id);

    /** Find all intents by suggestion type (for admin/analytics). */
    List<SearchIntent> findBySuggestionType(String suggestionType);
}
