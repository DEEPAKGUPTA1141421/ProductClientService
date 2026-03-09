package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.SellerPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SellerPreferencesRepository extends JpaRepository<SellerPreferences, UUID> {
    Optional<SellerPreferences> findBySellerId(UUID sellerId);
}