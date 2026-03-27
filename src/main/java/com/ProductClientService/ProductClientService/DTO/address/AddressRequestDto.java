package com.ProductClientService.ProductClientService.DTO.address;

import java.math.BigDecimal;

import com.ProductClientService.ProductClientService.Model.Address.AddressType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for POST /api/v1/users/addresses
 * and PUT /api/v1/users/addresses/{id}.
 *
 * All fields mirror the API spec table exactly.
 */
public record AddressRequestDto(

        @NotNull(message = "address_type is required") AddressType addressType,

        @NotBlank(message = "full_name is required") @Size(max = 150, message = "full_name must not exceed 150 characters") String fullName,

        @NotBlank(message = "phone is required") @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "phone must be a valid number (7-15 digits, optional leading +)") String phone,

        @NotBlank(message = "line1 is required") @Size(max = 255, message = "line1 must not exceed 255 characters") String line1,

        @Size(max = 255, message = "line2 must not exceed 255 characters") String line2,

        @Size(max = 255, message = "landmark must not exceed 255 characters") String landmark,

        @NotBlank(message = "city is required") String city,

        @NotBlank(message = "state is required") String state,

        @NotBlank(message = "pincode is required") @Pattern(regexp = "^[0-9]{6}$", message = "pincode must be exactly 6 digits") String pincode,

        Boolean isDefault,

        // Optional GPS coordinates for a map pin
        BigDecimal lat,
        BigDecimal lng

) {
}