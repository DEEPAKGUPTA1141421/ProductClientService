package com.ProductClientService.ProductClientService.DTO.Settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record PersonalInfoDto(

                @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters") String fullName,

                @Size(max = 50, message = "Display name cannot exceed 50 characters") String displayName,

                @Email(message = "Invalid email format") @Size(max = 150, message = "Email cannot exceed 150 characters") String email,

                @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10 digit Indian mobile number") String phone,

                @Size(max = 255, message = "Address cannot exceed 255 characters") String address,
                BigDecimal latitude,
                BigDecimal longitude,
                List<MultipartFile> mediaFiles,
                MultipartFile profileImage) {
}
// uo hui iuui huiuin