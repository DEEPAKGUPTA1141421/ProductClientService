package com.ProductClientService.ProductClientService.Repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerAddress;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {
    // Additional query methods can be defined here if

    @Autowired
    SellerAddressRepository sellerAddressRepository = null;

    Optional<Seller> findByPhone(String phone);

    default Seller findOrCreateByPhone(String phone) {
        return findByPhone(phone).orElseGet(() -> {
            Seller seller = new Seller();
            seller.setPhone(phone);
            return save(seller);
        });
    }

    default Seller saveBasicInfo(String phone, String display_name, String legal_name, String email) {
        Optional<Seller> optionalSeller = findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return null;
        }
        Seller seller = optionalSeller.get();
        // update fields
        seller.setDisplayName(display_name);
        seller.setLegalName(legal_name);
        seller.setEmail(email);
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.BASIC_INFO_NAME);

        return save(seller);
    }

    default boolean stageValidation(Seller.ONBOARDSTAGE stage, String phone) {
        Optional<Seller> optionalSeller = findByPhone(phone);
        if (optionalSeller.isEmpty())
            return false;
        return stage == optionalSeller.get().getOnboardingStage();
    }
}
