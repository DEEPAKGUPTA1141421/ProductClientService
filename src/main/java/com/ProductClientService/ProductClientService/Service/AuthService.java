package com.ProductClientService.ProductClientService.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.LoginRequest;
import com.ProductClientService.ProductClientService.DTO.NotificationRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.CreateRiderDto;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.RiderIdResponse;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Seller.ONBOARDSTAGE;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Repository.OtpRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;
import com.ProductClientService.ProductClientService.Utils.RateLimiter;
import com.ProductClientService.ProductClientService.network.DeliveryInventoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final ObjectMapper objectMapper;
    private final SellerRepository sellerRepository;
    private final SellerAddressRepository sellerAddressRepository;
    private final OtpRepository otpRepository;
    private final RateLimiter rateLimiter;
    private final KafkaProducerService producerService;
    private final JwtService jwtService;
    private final HttpServletRequest request;
    private final ObjectProvider<GoogleMapsService> googleMapsProvider;
    private final DeliveryInventoryClient deliveryInventoryClient;
    private final UserRepojectory userRepojectory;

    public ApiResponse<String> login(LoginRequest loginRequest) {
        // Check rate limit
        System.out.println("Checking rate limit for phone: ");
        if (!rateLimiter.allow(loginRequest.phone())) {
            System.out.println("Rate limit exceeded for phone: " + loginRequest.phone());
            return new ApiResponse<>(false, "Too many requests. Please try again later.", null, 429);
        }
        if (loginRequest.typeOfUser() == LoginRequest.UserType.SELLER) {
            sellerRepository.findOrCreateByPhone(loginRequest.phone());
        } else if (loginRequest.typeOfUser() == LoginRequest.UserType.USER) {
            userRepojectory.findOrCreateByPhone(loginRequest.phone());
        } else if (loginRequest.typeOfUser() == LoginRequest.UserType.RIDER) {
            // Rider logic here
        } else {
            return new ApiResponse<>(true, "Invalid User Type", null, 403);
        }
        sendOtpAsync(loginRequest.phone(), "login");
        return new ApiResponse<>(true, "Otp Triggered on you Phone", null, 200);
    }

    public ApiResponse<String> verify(AuthRequest authrequest) {
        boolean valid = otpRepository.checkOtpValidity(authrequest.phone(), authrequest.otp_code(), "login");
        if (!valid)
            return new ApiResponse<>(false, "Otp is Invalid", null, 200);
        String token;
        if (authrequest.typeOfUser() == AuthRequest.UserType.SELLER) {
            Seller seller = sellerRepository.findOrCreateByPhone(authrequest.phone());
            token = jwtService.generateToken(authrequest.phone(), "SELLER", seller.getId());
        } else if (authrequest.typeOfUser() == AuthRequest.UserType.USER) {
            User user = userRepojectory.findOrCreateByPhone(authrequest.phone());
            token = jwtService.generateToken(authrequest.phone(), "USER", user.getId());
        } else if (authrequest.typeOfUser() == AuthRequest.UserType.RIDER) {
            // will handle rider case
            ApiResponse<RiderIdResponse> response = deliveryInventoryClient.createRiderWithPhone(
                    new CreateRiderDto("PHONE", authrequest.phone()));

            if (response.statusCode() == 200 && response.success()) {
                UUID riderId = response.data().id();
                token = jwtService.generateToken(authrequest.phone(), "RIDER", riderId);
            } else {
                throw new RuntimeException("Failed to create rider: " + response.message());
            }
        } else {
            return new ApiResponse<>(true, "Invalid User Type", null, 403);
        }
        return new ApiResponse<>(true, "Otp Verification Success", token, 200);
    }

    public ApiResponse<Seller> sellerBasicInfoVerify(SellerBasicInfo inforequest) {
        if (inforequest.stage_of_onboarding() == ONBOARDSTAGE.BASIC_INFO_NAME)
            return handleBasicNameInfo(inforequest);
        else if (inforequest.stage_of_onboarding() == ONBOARDSTAGE.LOCATION)
            return handleLocaton(inforequest);
        else if (inforequest.stage_of_onboarding() == ONBOARDSTAGE.ADHADHAR_CARD)
            return handleAdhadharCard(inforequest);
        else if (inforequest.stage_of_onboarding() == ONBOARDSTAGE.PAN_CARD)
            return handlePanCard(inforequest);
        else
            return new ApiResponse<>(false, null, null, 403);
    }

    @Async
    public void sendOtpAsync(String phone, String type) {
        String otpCode = generateOtp(); // Implement your OTP generation logic
        otpRepository.CreateOtp(phone, type, otpCode);
        System.out.println("OTP created asynchronously for phone: " + phone);
        NotificationRequest request = createNotificationBody(
                "Login Otp",
                "Otp For Login Is" + otpCode + " Please Do not share with anyone",
                phone,
                "sms");
        try {
            String json = objectMapper.writeValueAsString(request);
            producerService.sendMessage("notification", json); // send as String
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateOtp() {
        int otp = (int) (Math.random() * 9000) + 100000; // 6-digit OTP
        return String.valueOf(otp);
    }

    private NotificationRequest createNotificationBody(String subject, String body, String to, String type) {
        NotificationRequest request = new NotificationRequest();
        request.setTo(to);
        request.setSubject(subject);
        request.setBody(body);
        request.setType(type);
        return request;
    }

    private ApiResponse<Seller> handleBasicNameInfo(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        Seller seller = sellerRepository.saveBasicInfo(phone, inforequest.display_name(), inforequest.legal_name(),
                inforequest.email());
        System.out.println("seller details" + seller + phone);
        return new ApiResponse<>(true, "Basic Info Saved", seller, 200);
    }

    private ApiResponse<Seller> handleLocaton(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        if (!sellerRepository.stageValidation(Seller.ONBOARDSTAGE.BASIC_INFO_NAME, phone)) {
            return new ApiResponse<>(false, "Stage is Not Correct", null, 403);
        }
        System.out.println("calling google service and test" + inforequest.latitude().getClass()
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
        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return false;
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.LOCATION);

        sellerAddressRepository.saveOrUpdateLocationAddress(
                seller,
                addressDetails.line1(),
                addressDetails.city(),
                addressDetails.state(),
                addressDetails.country(),
                addressDetails.pincode(),
                lat,
                longi);
        return true;
    }

    private ApiResponse<Seller> handleAdhadharCard(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        if (!sellerRepository.stageValidation(Seller.ONBOARDSTAGE.LOCATION, phone)) {
            return new ApiResponse<>(false, "Stage is Not Correct", null, 403);
        }

        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return new ApiResponse<>(true, "Invalid Phone", null, 200);
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.ADHADHAR_CARD);

        sellerAddressRepository.saveOrUpdateAadharAddress(seller, inforequest.adhadhar_card());
        return new ApiResponse<>(true, "Adhadhar Verification Complete", null, 200);
    }

    private ApiResponse<Seller> handlePanCard(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        if (!sellerRepository.stageValidation(Seller.ONBOARDSTAGE.ADHADHAR_CARD, phone)) {
            return new ApiResponse<>(false, "Stage is Not Correct", null, 403);
        }

        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return new ApiResponse<>(true, "Invalid Phone", null, 200);
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.DOCUMENT_VERIFIED);

        sellerAddressRepository.saveOrUpdatePanAddress(seller, inforequest.pan_card());
        return new ApiResponse<>(true, "PanCard Verification Complete", null, 200);
    }

}

// huyh hihi hyihi hyh huih huihu huj ggygggygggggg

// njkhuiitgiugtuhug
