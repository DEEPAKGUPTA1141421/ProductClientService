package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.SellerNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SellerNotificationPreferencesRepository extends JpaRepository<SellerNotificationPreferences, UUID> {
    Optional<SellerNotificationPreferences> findBySellerId(UUID sellerId);
}