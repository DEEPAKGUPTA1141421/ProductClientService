package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomSellerDetailsService implements UserDetailsService{
    private final SellerRepository sellerRepository;

    public CustomSellerDetailsService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        var seller = sellerRepository.findByPhone(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert your User entity into Spring Security's UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username("deepak")
                .password("12345678") // must be encoded
                .roles("seller") // like ROLE_USER, ROLE_ADMIN
                .build();
    }
}


