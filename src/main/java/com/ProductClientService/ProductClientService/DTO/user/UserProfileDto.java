package com.ProductClientService.ProductClientService.DTO.user;

import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Model.Address;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private String dateOfBirth; // ISO 8601 string
    private String avatarUrl;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private String loyaltyTier;
    private Integer loyaltyPoints;
    private Double walletBalance;
    private String referralCode;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastLoginAt;

    // Nested address list
    private List<AddressDto> addresses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressDto {
        private UUID id;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String country;
        private String pincode;
        private boolean isDefault;
        private double latitude;
        private double longitude;
    }

    public static UserProfileDto fromEntity(User user) {
        List<AddressDto> addressDtos = user.getAddresses() == null ? List.of()
                : user.getAddresses().stream()
                        .map(a -> AddressDto.builder()
                                .id(a.getId())
                                .line1(a.getLine1())
                                .line2(a.getLine2())
                                .city(a.getCity())
                                .state(a.getState())
                                .country(a.getCountry())
                                .pincode(a.getPincode())
                                .isDefault(a.isDefault())
                                .latitude(a.getLatitude() != null ? a.getLatitude().doubleValue() : 0)
                                .longitude(a.getLongitude() != null ? a.getLongitude().doubleValue() : 0)
                                .build())
                        .toList();

        // Split name into first/last (stored as single "name" field in your User model)
        String[] nameParts = user.getName() != null ? user.getName().split(" ", 2) : new String[] { "", "" };
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        return UserProfileDto.builder()
                .id(user.getId())
                .firstName(firstName)
                .lastName(lastName)
                .phone(user.getPhone())
                .email(user.getEmail()) // add email field to User (see below)
                .avatarUrl(user.getAvatarUrl()) // add avatarUrl field to User (see below)
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .isEmailVerified(user.isEmailVerified())
                .isPhoneVerified(user.getStatus() == User.UserStatus.ACTIVE)
                .loyaltyTier("None")
                .loyaltyPoints(0)
                .walletBalance(0.0)
                .referralCode("REF-" + user.getId().toString().substring(0, 6).toUpperCase())
                .createdAt(user.getCreatedAt())
                .addresses(addressDtos)
                .build();
    }
}
