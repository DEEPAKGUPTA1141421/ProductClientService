package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepojectory extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = "INSERT INTO user_purchased_products (user_id, product_id) VALUES (:userId, :productId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void addPurchasedProduct(@Param("userId") UUID userId, @Param("productId") UUID productId);
}
