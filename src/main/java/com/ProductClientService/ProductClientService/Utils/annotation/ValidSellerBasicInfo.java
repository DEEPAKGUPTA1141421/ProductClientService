package com.ProductClientService.ProductClientService.Utils.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import com.ProductClientService.ProductClientService.Utils.validator.SellerBasicInfoValidator;

@Documented
@Constraint(validatedBy = SellerBasicInfoValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSellerBasicInfo{
    String message() default "Invalid Seller Basic Info for the given onboarding stage";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
