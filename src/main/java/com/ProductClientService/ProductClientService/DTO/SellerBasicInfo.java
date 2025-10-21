package com.ProductClientService.ProductClientService.DTO;

import java.math.BigDecimal;

import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Seller.ONBOARDSTAGE;
import com.ProductClientService.ProductClientService.Utils.annotation.ValidSellerBasicInfo;

import jakarta.validation.constraints.Email;

@ValidSellerBasicInfo
public record SellerBasicInfo(
                String legal_name,
                String display_name,

                @Email(message = "Invalid email format") String email,

                BigDecimal latitude,
                BigDecimal longitude,

                String adhadhar_card,
                String pan_card,
                Seller.ShopCategory category,
                ONBOARDSTAGE stage_of_onboarding) {
}
