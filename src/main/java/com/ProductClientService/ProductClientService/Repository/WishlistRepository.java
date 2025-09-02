package com.ProductClientService.ProductClientService.Repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    Optional<Wishlist> findByUserId(UUID userId);
}
