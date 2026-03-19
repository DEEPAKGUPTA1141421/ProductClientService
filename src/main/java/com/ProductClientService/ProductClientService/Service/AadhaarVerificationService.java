package com.ProductClientService.ProductClientService.Service;

import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.NotificationRequest;
import com.ProductClientService.ProductClientService.DTO.Settings.AadhaarVerificationDto;
import com.ProductClientService.ProductClientService.Model.Otp;
import com.ProductClientService.ProductClientService.Repository.OtpRepository;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class AadhaarVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(AadhaarVerificationService.class);
    private final RestTemplate restTemplate;
    private final OtpRepository otpRepository;
    private final ObjectMapper objectMapper;
    private final KafkaProducerService producerService;

    @Value("${aadhaar.verification.bypass}")
    private boolean bypassAadhaarVerification;

    @Value("${aadhaar.api.endpoint:https://api.example.com/aadhaar}")
    private String aadhaarApiEndpoint;

    @Value("${aadhaar.api.key:}")
    private String aadhaarApiKey;

    /**
     * Trigger Aadhaar OTP - Calls external API for Aadhaar verification and sends
     * OTP to Kafka
     */
    public ApiResponse<Object> triggerAadhaarOtp(AadhaarVerificationDto request) {
        try {
            // Get phone number from UserPrincipal (JWT token)
            String phoneNumber = getPhoneNumberFromPrincipal();

            logger.info("Triggering Aadhaar OTP for aadhaar: {} and phone: {}", maskAadhaar(request.aadharNumber()),
                    phoneNumber);

            // Validate Aadhaar with external API
            boolean isAadhaarValid = validateAadhaarWithExternalApi(request.aadharNumber(),
                    phoneNumber);

            if (!bypassAadhaarVerification || isAadhaarValid) {
                logger.warn("Aadhaar validation failed for phone: {}", phoneNumber);
                return new ApiResponse<>(false, "Invalid Aadhaar number or phone mismatch", null, 400);
            }

            // Generate and send OTP asynchronously
            sendOtpAsync(phoneNumber);

            logger.info("Aadhaar OTP request sent to Kafka for phone: {}", phoneNumber);

            return new ApiResponse<>(true, "OTP sent successfully to your registered mobile number",
                    null, 200);

        } catch (Exception e) {
            logger.error("Error triggering Aadhaar OTP", e);
            return new ApiResponse<>(false, "Error processing Aadhaar verification: " + e.getMessage(), null,
                    500);
        }
    }

    /**
     * Verify Aadhaar OTP
     */
    public ApiResponse<Object> verifyAadhaarOtp(String otp) {
        try {
            // Get phone number from UserPrincipal (JWT token)
            String phoneNumber = getPhoneNumberFromPrincipal();

            logger.info("Verifying OTP for phone: {}", phoneNumber);

            // Check OTP validity
            boolean isOtpValid = otpRepository.checkOtpValidity(phoneNumber, otp, "aadhaarVerification");

            if (!isOtpValid) {
                logger.warn("Invalid or expired OTP for phone: {}", phoneNumber);
                return new ApiResponse<>(false, "Invalid or expired OTP", null, 400);
            }

            // Mark OTP as verified
            Otp.typeOfOtp type = Otp.typeOfOtp.valueOf("aadhaarVerification");
            int updatedCount = otpRepository.markAsVerified(phoneNumber, otp, type);

            if (updatedCount == 0) {
                logger.warn("Failed to mark OTP as verified for phone: {}", phoneNumber);
                return new ApiResponse<>(false, "Failed to verify OTP", null, 500);
            }

            logger.info("OTP verified successfully for phone: {}", phoneNumber);

            return new ApiResponse<>(true, "Aadhaar verification completed successfully", phoneNumber, 200);

        } catch (Exception e) {
            logger.error("Error verifying Aadhaar OTP", e);
            return new ApiResponse<>(false, "Error verifying OTP: " + e.getMessage(), null, 500);
        }
    }

    /**
     * Validate Aadhaar with external API (e.g., Digilocker, Signzy, etc.)
     * 
     * @return true if Aadhaar is valid and matches phone, false otherwise
     */
    private boolean validateAadhaarWithExternalApi(String aadhaarNumber, String phoneNumber) {
        try {
            logger.info("Calling external Aadhaar verification API");

            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aadhaarApiKey);

            // Prepare request body
            AadhaarVerificationRequest verificationRequest = new AadhaarVerificationRequest(aadhaarNumber,
                    phoneNumber);
            HttpEntity<AadhaarVerificationRequest> entity = new HttpEntity<>(verificationRequest, headers);

            // Call external API
            ResponseEntity<AadhaarVerificationResponse> response = restTemplate
                    .postForEntity(aadhaarApiEndpoint + "/verify", entity, AadhaarVerificationResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AadhaarVerificationResponse verificationResponse = response.getBody();
                boolean isValid = verificationResponse.isValid() && verificationResponse.phoneMatches();
                logger.info("Aadhaar verification response: valid={}, phoneMatches={}", verificationResponse.isValid(),
                        verificationResponse.phoneMatches());
                return isValid;
            }

            logger.warn("External API returned non-success status: {}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            logger.error("Error calling external Aadhaar verification API", e);
            return false;
        }
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Get phone number from UserPrincipal in SecurityContext
     */
    private String getPhoneNumberFromPrincipal() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getPhone();
    }

    /**
     * Mask Aadhaar number for logging (show only last 4 digits)
     */
    private String maskAadhaar(String aadhaar) {
        if (aadhaar != null && aadhaar.length() >= 4) {
            return "****" + aadhaar.substring(aadhaar.length() - 4);
        }
        return "****";
    }

    /**
     * Send OTP asynchronously via Kafka
     */
    @Async
    public void sendOtpAsync(String phoneNumber) {
        try {
            String otpCode = generateOtp();
            otpRepository.CreateOtp(phoneNumber, "aadhaarVerification", otpCode);
            logger.info("OTP created asynchronously for phone: {} and OTP: {}", phoneNumber, otpCode);

            NotificationRequest request = new NotificationRequest();
            request.setTo(phoneNumber);
            request.setSubject("Aadhaar Verification OTP");
            request.setBody("Your Aadhaar verification OTP is: " + otpCode
                    + ". Valid for 5 minutes. Please do not share with anyone.");
            request.setType("sms");

            String json = objectMapper.writeValueAsString(request);
            producerService.sendMessage("notification", json);
            logger.info("OTP notification sent to Kafka for phone: {}", phoneNumber);
        } catch (Exception e) {
            logger.error("Error sending OTP async for phone: {}", phoneNumber, e);
        }
    }

    // Inner classes for API requests/responses
    public static class AadhaarVerificationRequest {
        public String aadhaarNumber;
        public String phoneNumber;

        public AadhaarVerificationRequest(String aadhaarNumber, String phoneNumber) {
            this.aadhaarNumber = aadhaarNumber;
            this.phoneNumber = phoneNumber;
        }

        public String getAadhaarNumber() {
            return aadhaarNumber;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
    }

    public static class AadhaarVerificationResponse {
        private boolean valid;
        private boolean phoneMatches;
        private String name;
        private String aadhaarNumber;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean phoneMatches() {
            return phoneMatches;
        }

        public void setPhoneMatches(boolean phoneMatches) {
            this.phoneMatches = phoneMatches;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAadhaarNumber() {
            return aadhaarNumber;
        }

        public void setAadhaarNumber(String aadhaarNumber) {
            this.aadhaarNumber = aadhaarNumber;
        }
    }

    public static class SmsRequest {
        public String phone;
        public String message;

        public SmsRequest(String phone, String message) {
            this.phone = phone;
            this.message = message;
        }

        public String getPhone() {
            return phone;
        }

        public String getMessage() {
            return message;
        }
    }
}
// huihbhhu