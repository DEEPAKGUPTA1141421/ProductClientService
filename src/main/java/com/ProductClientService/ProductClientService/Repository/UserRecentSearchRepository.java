package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.UserRecentSearch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRecentSearchRepository extends JpaRepository<UserRecentSearch, UUID> {

    List<UserRecentSearch> findTop10ByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<UserRecentSearch> findByUserIdAndItemIdAndItemType(UUID userId, String itemId,
            UserRecentSearch.ItemType itemType);

    void deleteByUserIdAndIdNotIn(UUID userId, List<UUID> keepIds);
}
