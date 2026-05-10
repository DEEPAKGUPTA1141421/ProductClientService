package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ShopFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopFollowRepository extends JpaRepository<ShopFollow, UUID> {

    long countBySellerId(UUID sellerId);

    boolean existsByUserIdAndSellerId(UUID userId, UUID sellerId);

    Optional<ShopFollow> findByUserIdAndSellerId(UUID userId, UUID sellerId);

    void deleteByUserIdAndSellerId(UUID userId, UUID sellerId);
}
