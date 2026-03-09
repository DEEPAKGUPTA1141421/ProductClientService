package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.SellerBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SellerBankDetailsRepository extends JpaRepository<SellerBankDetails, UUID> {
    Optional<SellerBankDetails> findBySellerId(UUID sellerId);
}