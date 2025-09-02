package com.ProductClientService.ProductClientService.Utils.validator;

import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.Model.Seller.ONBOARDSTAGE;
import com.ProductClientService.ProductClientService.Utils.annotation.ValidSellerBasicInfo;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SellerBasicInfoValidator implements ConstraintValidator <ValidSellerBasicInfo, SellerBasicInfo> {
    @Override
    public boolean isValid(SellerBasicInfo info, ConstraintValidatorContext context) {
        if (info == null) return true;

        ONBOARDSTAGE stage = info.stage_of_onboarding();

        switch (stage) {
            case BASIC_INFO_NAME -> {
                return notBlank(info.legal_name()) && notBlank(info.display_name()) && notBlank(info.email());
            }
            case LOCATION -> {
                return info.latitude() != null && info.longitude() != null;
            }
            case ADHADHAR_CARD -> {
                return notBlank(info.adhadhar_card()) && info.adhadhar_card().matches("\\d{12}");
            }
            case PAN_CARD -> {
                return notBlank(info.pan_card()) && info.pan_card().matches("[A-Z]{5}[0-9]{4}[A-Z]{1}");
            }
            default -> {
                return true;
            }
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
