package com.ProductClientService.ProductClientService.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ProductClientService.ProductClientService.Model.Cart;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndStatus(UUID userId, Cart.Status status);

    @Query("SELECT ci.productId FROM CartItem ci WHERE ci.cart.userId = :userId AND ci.cart.status = :status")
    Set<UUID> findProductIdsByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") Cart.Status status);
}
