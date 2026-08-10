package com.ProductClientService.ProductClientService.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ProductClientService.ProductClientService.Model.Wishlist;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    Optional<Wishlist> findByUserId(UUID userId);

    @Query("SELECT wi.productId FROM WishlistItem wi WHERE wi.wishlist.userId = :userId")
    Set<UUID> findProductIdsByUserId(@Param("userId") UUID userId);
}
