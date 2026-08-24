package com.ProductClientService.ProductClientService.Service.UserAuthStrategy;

import com.ProductClientService.ProductClientService.DTO.Auth.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.Auth.AuthResult;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SellerAuthStrategy implements UserAuthStrategy {
    private final SellerRepository sellerRepository;

    @Override
    public AuthRequest.UserType getUserType() {
        return AuthRequest.UserType.SELLER;
    }

    @Override
    public AuthResult processAuthentication(AuthRequest request) {
        Seller seller = sellerRepository.findByPhone(request.phone())
                .orElseThrow(() -> new EntityNotFoundException("Seller not found"));

        return new AuthResult(seller.getId(), "SELLER", seller);
    }

    @Override
    public boolean createUser(String phone) {
        boolean isSignup = false;
        Optional<Seller> seller = sellerRepository.findByPhone(phone);
        if (seller.isEmpty()) {
            Seller newSeller = new Seller(phone);
            sellerRepository.save(newSeller);
            isSignup = true;
        }
        return isSignup;
    }
}
