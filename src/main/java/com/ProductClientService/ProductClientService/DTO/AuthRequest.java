package com.ProductClientService.ProductClientService.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(

                @NotBlank(message = "Phone number is required") @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits") String phone,

                @NotBlank(message = "otp_code is required") @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits") String otp_code,

                @NotNull(message = "User type is required") UserType typeOfUser) {
        public enum UserType {
                SELLER,
                USER,
                RIDER
        }

}
