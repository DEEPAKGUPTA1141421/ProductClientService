package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.SharedWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedWishlistRepository extends JpaRepository<SharedWishlist, UUID> {
    Optional<SharedWishlist> findByToken(String token);

    Optional<SharedWishlist> findByOwnerIdAndExpiresAtIsNull(UUID ownerId);
}