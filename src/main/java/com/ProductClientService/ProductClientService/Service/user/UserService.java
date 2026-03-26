package com.ProductClientService.ProductClientService.Service.user;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.NotificationRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.user.UpdateEmailRequest;
import com.ProductClientService.ProductClientService.DTO.user.UpdateProfileRequest;
import com.ProductClientService.ProductClientService.DTO.user.UserProfileDto;
import com.ProductClientService.ProductClientService.DTO.user.VerifyEmailOtpRequest;
import com.ProductClientService.ProductClientService.Model.Address;
import com.ProductClientService.ProductClientService.Model.Otp;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Model.UserRecentSearch;
import com.ProductClientService.ProductClientService.Repository.OtpRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.UserRecentSearchRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService;
import com.ProductClientService.ProductClientService.Service.KafkaProducerService;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;
import com.ProductClientService.ProductClientService.Service.OpenStreetMapService;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ObjectProvider<GoogleMapsService> googleMapsProvider;
    private final HttpServletRequest request;
    private final UserRepojectory userRepojectory;
    private final SellerAddressRepository sellerAddressRepository;
    private final UserRecentSearchRepository repo;
    private final OtpRepository otpRepository;
    private final KafkaProducerService producerService;
    private final ObjectMapper objectMapper;

    private final Cloudinary cloudinary;

    public ApiResponse<Object> getFullProfile() {
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new ApiResponse<>(true, "Profile fetched", UserProfileDto.fromEntity(user), 200);
    }

    @Transactional
    public ApiResponse<Object> updateProfile(UpdateProfileRequest dto) {
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Merge first + last name back into the single "name" field
        if (dto.firstName() != null || dto.lastName() != null) {
            String first = dto.firstName() != null ? dto.firstName() : "";
            String last = dto.lastName() != null ? dto.lastName() : "";
            user.setName((first + " " + last).trim());
        }
        if (dto.gender() != null)
            user.setGender(dto.gender());
        if (dto.dateOfBirth() != null)
            user.setDateOfBirth(dto.dateOfBirth());

        userRepojectory.save(user);
        return new ApiResponse<>(true, "Profile updated successfully",
                UserProfileDto.fromEntity(user), 200);
    }

    @Transactional
    public ApiResponse<Object> updateAvatar(MultipartFile file) {
        try {
            User user = userRepojectory.findById(getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String uploadedUrl;
            try {
                Map uploadResult = cloudinary.uploader()
                        .upload(file.getBytes(), ObjectUtils.emptyMap());
                uploadedUrl = uploadResult.get("url").toString();
            } catch (IOException e) {
                return new ApiResponse<>(false,
                        "Failed to upload image for User Avatar ",
                        null,
                        500);
            }
            user.setAvatarUrl(uploadedUrl);
            userRepojectory.save(user);

            return new ApiResponse<>(true, "Avatar updated", Map.of("avatarUrl", uploadedUrl), 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Upload failed: " + e.getMessage(), null, 500);
        }
    }

    @Transactional
    public ApiResponse<Object> requestEmailUpdate(UpdateEmailRequest dto) {
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check new email isn't already taken by another account
        if (userRepojectory.findByEmail(dto.email()).isPresent()) {
            return new ApiResponse<>(false, "Email already in use by another account", null, 409);
        }

        // Store in pending_email — don't commit yet
        user.setPendingEmail(dto.email());
        userRepojectory.save(user);

        // Send OTP to the NEW email address
        sendEmailOtpAsync(dto.email(), user.getPhone());

        return new ApiResponse<>(true,
                "OTP sent to " + dto.email() + ". Please verify to complete the update.", null, 200);
    }

    @Async
    public void sendEmailOtpAsync(String toEmail, String phone) {
        try {
            String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);
            otpRepository.CreateOtp(phone, "emailVerification", otpCode);

            NotificationRequest notification = new NotificationRequest();
            notification.setTo(toEmail);
            notification.setSubject("Verify your email - OTP");
            notification.setBody("Your email verification OTP is: " + otpCode
                    + ". Valid for 5 minutes. Do not share with anyone.");
            notification.setType("email");

            producerService.sendMessage("notification", objectMapper.writeValueAsString(notification));
        } catch (Exception e) {
            System.err.println("Failed to send email OTP: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Object> verifyEmailOtp(VerifyEmailOtpRequest dto) {
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPendingEmail() == null) {
            return new ApiResponse<>(false, "No pending email update found. Please request again.", null, 400);
        }

        // Check OTP validity (keyed to phone, type = emailVerification)
        boolean valid = otpRepository.checkOtpValidity(
                user.getPhone(), dto.otp(), "emailVerification");

        if (!valid) {
            return new ApiResponse<>(false, "Invalid or expired OTP", null, 400);
        }

        // Commit the email change
        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerified(true);
        userRepojectory.save(user);

        otpRepository.markAsVerified(user.getPhone(), dto.otp(), Otp.typeOfOtp.emailVerification);

        return new ApiResponse<>(true, "Email verified and updated successfully",
                Map.of("email", user.getEmail()), 200);
    }

    @Transactional
    public ApiResponse<Object> requestAccountDeletion() {
        User user = userRepojectory.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft-delete: mark as INACTIVE (you can add a scheduled job to hard-delete
        // after 30 days)
        user.setStatus(User.UserStatus.INACTIVE);
        userRepojectory.save(user);

        return new ApiResponse<>(true,
                "Account scheduled for deletion. You can reactivate by logging in within 30 days.", null, 200);
    }

    public ApiResponse<Object> handleLocaton(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        System.out.println("calling google service and test" +
                inforequest.latitude().getClass()
                + inforequest.longitude().getClass() + "hello and say");
        GoogleMapsService googleMapsService = googleMapsProvider.getObject();
        AddressResponse addressDetails = googleMapsService.getAddressFromLatLng(
                inforequest.latitude(),
                inforequest.longitude());
        System.out.println("we are calling repo");
        boolean isSaved = saveAddress(addressDetails, phone, inforequest.latitude(),
                inforequest.longitude());
        if (!isSaved)
            return new ApiResponse<>(false, "Location Info Not Saved", null, 500);
        return new ApiResponse<>(true, "Location Info Saved", null, 200);
    }

    private boolean saveAddress(AddressResponse addressDetails, String phone, BigDecimal lat, BigDecimal longi) {
        User user = userRepojectory.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Address address = new Address();
        System.out.println("City is " + addressDetails.city() + addressDetails);
        address.setCity(addressDetails.city());
        address.setLine1(addressDetails.line1());
        address.setState(addressDetails.state());
        address.setCountry(addressDetails.country());
        address.setPincode(addressDetails.pincode());
        address.setUser(user);
        address.setLatitude(lat);
        address.setLongitude(longi);
        sellerAddressRepository.save(address);
        return true;
    }

    public ApiResponse<Object> searchPlace(String keyword) {
        try {
            GoogleMapsService googleMapsService = googleMapsProvider.getObject();
            List<AddressResponse> addressDetails = googleMapsService.searchPlaces(keyword);
            return new ApiResponse<>(true, "Search Result", addressDetails, 201);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Search Failed", null, 501);
        }
    }

    @Transactional
    public ApiResponse<Object> setDefaultAddress(UUID addressId) {
        try {
            // Get current user from request attribute
            User user = userRepojectory.findById(getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!addressBelongToUser(user, addressId)) {
                throw new RuntimeException("Address does not belong to the user");
            }
            List<Address> addresses = user.getAddresses();
            for (Address addr : addresses) {
                addr.setDefault(addr.getId().equals(addressId));
            }

            sellerAddressRepository.saveAll(addresses);

            return new ApiResponse<>(true, "Default address updated successfully", user, 200);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    private boolean addressBelongToUser(User user, UUID addressId) {
        List<Address> addresses = user.getAddresses();

        // Flag to track if the address belongs to user
        boolean found = false;

        for (Address addr : addresses) {
            if (addr.getId().equals(addressId)) {
                found = true;
                break; // stop the loop once found
            }
        }
        return found;
    }

    @Transactional
    public void saveSearch(String itemId, UserRecentSearch.ItemType itemType,
            String title, String imageUrl, String meta) {
        // Check if this item already exists
        var existing = repo.findByUserIdAndItemIdAndItemType(getUserId(), itemId, itemType);

        if (existing.isPresent()) {
            UserRecentSearch search = existing.get();
            search.setCountOfSearch(search.getCountOfSearch() + 1);
            repo.save(search);
        } else {
            UserRecentSearch newSearch = new UserRecentSearch();
            newSearch.setUserId(getUserId());
            newSearch.setItemId(itemId);
            newSearch.setItemType(itemType);
            newSearch.setTitle(title);
            newSearch.setImageUrl(imageUrl);
            newSearch.setMeta(meta);
            repo.save(newSearch);
        }

        // Keep only last 10 searches
        List<UserRecentSearch> last10 = repo.findTop10ByUserIdOrderByUpdatedAtDesc(getUserId());
        List<UUID> last10Ids = last10.stream().map(UserRecentSearch::getId).collect(Collectors.toList());

        repo.deleteByUserIdAndIdNotIn(getUserId(), last10Ids);
    }

    public List<UserRecentSearch> getLastSearches() {
        return repo.findTop10ByUserIdOrderByUpdatedAtDesc(getUserId());
    }

    private UUID getUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}

// hhhhunhgj hvuyg yguy hjbjhh hbguj jhguygguhjhhnjhgyu yhfuhgfhj jhguyj
// gjubhjguhn kjnkjnkjnknikhiuhyi7y
// huiy8i9u hiyikjhiuhihhuiiojiojukjknj bhkj bhbjkjhkjhkkujjkijijjlnj