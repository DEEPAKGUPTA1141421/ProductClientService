package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ProductClientService.ProductClientService.Model.UserRecentSearch;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRecentSearchRepository extends JpaRepository<UserRecentSearch, UUID> {

    List<UserRecentSearch> findTop10ByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<UserRecentSearch> findByUserIdAndItemIdAndItemType(UUID userId, String itemId,
            UserRecentSearch.ItemType itemType);

    void deleteByUserIdAndIdNotIn(UUID userId, List<UUID> keepIds);

    // 🔹 Get overall top most searched items (PRODUCT or BRAND)
    @Query(value = """
                SELECT
                    item_id AS itemId,
                    item_type AS itemType,
                    title,
                    image_url AS imageUrl,
                    SUM(count_of_search) AS totalSearches
                FROM user_recent_searches
                WHERE created_at >= :since
                GROUP BY item_id, item_type, title, image_url
                ORDER BY totalSearches DESC
                LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopMostSearchedItems(@Param("since") ZonedDateTime since, @Param("limit") int limit);
}
