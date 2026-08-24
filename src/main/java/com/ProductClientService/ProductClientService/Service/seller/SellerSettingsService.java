package com.ProductClientService.ProductClientService.Service.seller;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.NotificationRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.Settings.BankDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.BusinessDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.ChangePasswordDto;
import com.ProductClientService.ProductClientService.DTO.Settings.NotificationPreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PersonalInfoDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.SecurityQuestionsDto;
import com.ProductClientService.ProductClientService.DTO.user.UpdateEmailRequest;
import com.ProductClientService.ProductClientService.DTO.user.VerifyEmailOtpRequest;
import com.ProductClientService.ProductClientService.Model.Otp;
import com.ProductClientService.ProductClientService.Model.RefreshToken;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.SellerBankDetails;
import com.ProductClientService.ProductClientService.Model.SellerNotificationPreferences;
import com.ProductClientService.ProductClientService.Model.SellerPreferences;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.OtpRepository;
import com.ProductClientService.ProductClientService.Repository.RefreshTokenRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerBankDetailsRepository;
import com.ProductClientService.ProductClientService.Repository.SellerNotificationPreferencesRepository;
import com.ProductClientService.ProductClientService.Repository.SellerPreferencesRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Service.BaseService;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService;
import com.ProductClientService.ProductClientService.Service.ImageUploadService;
import com.ProductClientService.ProductClientService.Service.KafkaProducerService;
//import com.ProductClientService.ProductClientService.Repository.SellerSessionRepository;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;
import com.ProductClientService.ProductClientService.filter.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class SellerSettingsService extends BaseService {

    private static final String ZONE_KOLKATA = "Asia/Kolkata";
    private static final long EMAIL_OTP_RESEND_COOLDOWN_SECONDS = 60;

    private final SellerRepository sellerRepository;
    private final SellerBankDetailsRepository bankDetailsRepository;
    private final SellerNotificationPreferencesRepository notificationRepository;
    private final SellerPreferencesRepository preferencesRepository;
    private final CategoryRepository CategoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpServletRequest request;
    private final EntityManager entityManager;
    private final GoogleMapsService googleMapsService;
    private final SellerAddressRepository sellerAddressRepository;
    private final ImageUploadService fileUploadService;
    private final ObjectMapper objectMapper;
    private final OtpRepository otpRepository;
    private final KafkaProducerService producerService;
    private static final Logger logger = LoggerFactory.getLogger(SellerSettingsService.class);

    // ──────────────────────────────────────────
    // Helper: get current seller UUID from request
    // ─────────────────────────────────────────────

    private Seller getSellerOrThrow() {
        return sellerRepository.findById(getUserId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
    }

    // ─────────────────────────────────────────────
    // GET ALL SETTINGS (single call for frontend)
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getAllSettings() {
        UUID sellerId = getUserId();

        Seller seller = getSellerOrThrow();
        Optional<SellerBankDetails> bank = bankDetailsRepository.findBySellerId(sellerId);
        Optional<SellerNotificationPreferences> notif = notificationRepository.findBySellerId(sellerId);
        Optional<SellerPreferences> prefs = preferencesRepository.findBySellerId(sellerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("personal", buildPersonalInfo(seller));
        data.put("business", buildBusinessDetails(seller));
        data.put("bank", bank.map(this::buildBankInfo).orElse(null));
        data.put("notifications", notif.map(this::buildNotifInfo).orElse(defaultNotifications()));
        data.put("preferences", prefs.map(this::buildPrefsInfo).orElse(defaultPreferences()));

        Seller.ONBOARDSTAGE stage = seller.getOnboardingStage();
        boolean onboardingComplete = stage == Seller.ONBOARDSTAGE.DOCUMENT_VERIFIED
                || stage == Seller.ONBOARDSTAGE.DOCUMENT_VERIFICATION_PENDING;
        data.put("onboardingStage", stage != null ? stage.name() : "REGISTER");
        data.put("onboardingComplete", onboardingComplete);

        return new ApiResponse<>(true, "Settings fetched", data, 200);
    }

    // ─────────────────────────────────────────────
    // PERSONAL INFO
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getPersonalInfo() {
        Seller seller = getSellerOrThrow();
        return new ApiResponse<>(true, "Personal info fetched", buildPersonalInfo(seller), 200);
    }

    @Transactional
    public ApiResponse<Object> updatePersonalInfo(PersonalInfoDto dto) {
        Seller seller = getSellerOrThrow();
        Seller.ONBOARDSTAGE currentStage = seller.getOnboardingStage();
        if (currentStage == null || currentStage.ordinal() < Seller.ONBOARDSTAGE.BASIC_INFO_NAME.ordinal()) {
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.BASIC_INFO_NAME);
            sellerRepository.save(seller);
        }
        if (dto.fullName() != null)
            seller.setLegalName(dto.fullName());
        if (dto.displayName() != null)
            seller.setDisplayName(dto.displayName());
        // Email changes are NOT accepted here — they must go through the
        // OTP-verified requestEmailUpdate()/verifyEmailOtp() flow below so a
        // seller can never assign themselves an unverified email address.

        // Update address if present
        if (dto.address() != null) {
            AddressResponse addressDetails = googleMapsService.getAddressFromLatLng(dto.latitude(), dto.longitude());
            boolean isSaved = saveAddress(addressDetails, dto.latitude(), dto.longitude());
        }

        // Profile photo upload
        if (dto.profileImage() != null && !dto.profileImage().isEmpty()) {
            try {
                String photoUrl = fileUploadService.uploadImage(dto.profileImage());
                logger.info("Uploaded profile photo: {}", photoUrl);
                seller.setProfilePhotoUrl(photoUrl);
            } catch (Exception e) {
                logger.warn("Profile photo upload skipped (check Cloudinary credentials): {}", e.getMessage());
            }
        }

        // Media files upload
        if (dto.mediaFiles() != null && !dto.mediaFiles().isEmpty()) {
            List<String> newUrls = new ArrayList<>();
            for (MultipartFile file : dto.mediaFiles()) {
                try {
                    String url = fileUploadService.uploadImage(file);
                    newUrls.add(url);
                    logger.info("Uploaded media file: {}", url);
                } catch (Exception e) {
                    logger.warn("Media file upload skipped ({}): {}", file.getOriginalFilename(), e.getMessage());
                }
            }
            if (!newUrls.isEmpty()) {
                try {
                    String existingMedia = seller.getProfileImageAndVideos();
                    List<String> existingUrls = existingMedia != null
                            ? objectMapper.readValue(existingMedia, List.class)
                            : new ArrayList<>();
                    existingUrls.addAll(newUrls);
                    seller.setProfileImageAndVideos(objectMapper.writeValueAsString(existingUrls));
                } catch (Exception e) {
                    logger.error("Error persisting media URLs: {}", e.getMessage());
                }
            }
        }
        sellerRepository.save(seller);
        return new ApiResponse<>(true, "Personal info updated", buildPersonalInfo(seller), 200);
    }

    // ─────────────────────────────────────────────
    // EMAIL CHANGE — OTP verified
    // ─────────────────────────────────────────────
    @Transactional
    public ApiResponse<Object> requestEmailUpdate(UpdateEmailRequest dto) {
        Seller seller = getSellerOrThrow();
        String newEmail = dto.email().trim().toLowerCase();

        if (newEmail.equalsIgnoreCase(seller.getEmail())) {
            return new ApiResponse<>(false, "This is already your current email", null, 400);
        }

        // Email must be unique across sellers (excluding the requesting seller)
        Optional<Seller> owner = sellerRepository.findByEmail(newEmail);
        if (owner.isPresent() && !owner.get().getId().equals(seller.getId())) {
            return new ApiResponse<>(false, "Email already in use by another account", null, 409);
        }

        // Resend cooldown — avoid OTP spam to the same phone/type
        Otp lastOtp = otpRepository.findTopByPhoneAndTypeOrderByCreatedAtDesc(seller.getPhone(),
                Otp.typeOfOtp.emailUpdate);
        if (lastOtp != null) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA));
            ZonedDateTime canResendAt = lastOtp.getCreatedAt().plusSeconds(EMAIL_OTP_RESEND_COOLDOWN_SECONDS);
            if (now.isBefore(canResendAt)) {
                long waitSeconds = java.time.Duration.between(now, canResendAt).getSeconds();
                return new ApiResponse<>(false,
                        "Please wait " + waitSeconds + "s before requesting another OTP", null, 429);
            }
        }

        // Stage the new email — it is NOT committed to seller.email until verified
        seller.setPendingEmail(newEmail);
        sellerRepository.save(seller);

        sendEmailOtpAsync(seller.getPhone(), newEmail);

        return new ApiResponse<>(true, "OTP sent to " + newEmail + ". Please verify to complete the update.",
                null, 200);
    }

    @Async
    public void sendEmailOtpAsync(String phone, String toEmail) {
        try {
            Otp otp = new Otp(phone, Otp.typeOfOtp.emailUpdate, false);
            otpRepository.save(otp);

            NotificationRequest notification = new NotificationRequest();
            notification.setTo(toEmail);
            notification.setSubject("Verify your new email");
            notification.setBody("Your Dashly Seller email verification OTP is: " + otp.getOtpCode()
                    + ". It is valid for 5 minutes. Do not share this code with anyone.");
            notification.setType("email");

            producerService.sendMessage("notification", objectMapper.writeValueAsString(notification));
        } catch (Exception e) {
            logger.error("Failed to send seller email OTP: {}", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Object> verifyEmailOtp(VerifyEmailOtpRequest dto) {
        Seller seller = getSellerOrThrow();

        if (seller.getPendingEmail() == null) {
            return new ApiResponse<>(false, "No pending email update found. Please request again.", null, 400);
        }

        Otp otp = otpRepository.findTopByPhoneAndTypeOrderByCreatedAtDesc(seller.getPhone(),
                Otp.typeOfOtp.emailUpdate);

        if (otp == null) {
            return new ApiResponse<>(false, "No OTP request found. Please request a new OTP.", null, 400);
        }
        if (otp.isVerified()) {
            return new ApiResponse<>(false, "This OTP has already been used. Please request a new one.", null, 400);
        }
        if (ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA)).isAfter(otp.getExpiryTime())) {
            return new ApiResponse<>(false, "OTP has expired. Please request a new one.", null, 400);
        }
        if (!otp.getOtpCode().equals(dto.otp())) {
            return new ApiResponse<>(false, "Invalid OTP code", null, 400);
        }

        // Re-check uniqueness at commit time in case it was claimed meanwhile
        Optional<Seller> owner = sellerRepository.findByEmail(seller.getPendingEmail());
        if (owner.isPresent() && !owner.get().getId().equals(seller.getId())) {
            seller.setPendingEmail(null);
            sellerRepository.save(seller);
            return new ApiResponse<>(false, "Email already in use by another account", null, 409);
        }

        seller.setEmail(seller.getPendingEmail());
        seller.setPendingEmail(null);
        seller.setEmailVerified(true);
        sellerRepository.save(seller);

        otpRepository.markAsVerified(seller.getPhone(), dto.otp(), Otp.typeOfOtp.emailUpdate);

        return new ApiResponse<>(true, "Email verified and updated successfully",
                Map.of("email", seller.getEmail()), 200);
    }

    private boolean saveAddress(AddressResponse addressDetails, BigDecimal lat, BigDecimal longi) {
        Optional<Seller> optionalSeller = sellerRepository.findById(getUserId());
        if (optionalSeller.isEmpty()) {
            return false;
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.LOCATION);

        sellerAddressRepository.saveOrUpdateLocationAddress(seller, addressDetails.line1(), addressDetails.city(),
                addressDetails.state(),
                addressDetails.country(),
                addressDetails.pincode(),
                lat,
                longi);
        return true;
    }

    // ─────────────────────────────────────────────
    // BUSINESS DETAILS
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getBusinessDetails() {
        Seller seller = getSellerOrThrow();
        return new ApiResponse<>(true, "Business details fetched", buildBusinessDetails(seller), 200);
    }

    @Transactional
    public ApiResponse<Object> updateBusinessDetails(BusinessDetailsDto dto) {
        Seller seller = getSellerOrThrow();
        if (seller.getOnboardingStage() == Seller.ONBOARDSTAGE.BASIC_INFO_NAME) {
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.BUSINESS_INFO);
        }
        Category category = CategoryRepository
                .findByIdAndCategoryLevel(dto.businessCategory(), Category.Level.SUPER_CATEGORY)
                .orElseThrow(() -> new RuntimeException("Invalid category"));
        seller.setCategory(category);
        if (dto.businessType() != null)
            seller.setBusinessType(dto.businessType());
        if (dto.gstNumber() != null)
            seller.setGstNumber(dto.gstNumber());

        sellerRepository.save(seller);
        return new ApiResponse<>(true, "Business details updated", buildBusinessDetails(seller), 200);
    }

    // ─────────────────────────────────────────────
    // BANK DETAILS
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getBankDetails() {
        UUID sellerId = getUserId();
        SellerBankDetails bank = bankDetailsRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Bank details not found"));
        return new ApiResponse<>(true, "Bank details fetched", buildBankInfo(bank), 200);
    }

    @Transactional
    public ApiResponse<Object> updateBankDetails(BankDetailsDto dto) {
        UUID sellerId = getUserId();
        Seller seller = getSellerOrThrow();
        if (seller.getOnboardingStage() == Seller.ONBOARDSTAGE.BUSINESS_INFO) {
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.BANK_ACCOUNT);
            sellerRepository.save(seller);
        }
        SellerBankDetails bank = bankDetailsRepository.findBySellerId(sellerId)
                .orElse(new SellerBankDetails());

        bank.setAccountHolderName(dto.accountHolderName());
        bank.setAccountNumber(dto.accountNumber());
        bank.setBankName(dto.bankName());
        bank.setIfscCode(dto.ifscCode());
        bank.setVerified(false); // re-verification needed on change
        bank.setSeller(entityManager.getReference(Seller.class, sellerId));

        bankDetailsRepository.save(bank);
        return new ApiResponse<>(true, "Bank details updated. Verification pending.", buildBankInfo(bank), 200);
    }

    // ─────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getNotificationPreferences() {
        UUID sellerId = getUserId();
        SellerNotificationPreferences notif = notificationRepository.findBySellerId(sellerId)
                .orElse(new SellerNotificationPreferences());
        return new ApiResponse<>(true, "Notification preferences fetched", buildNotifInfo(notif), 200);
    }

    @Transactional
    public ApiResponse<Object> updateNotificationPreferences(NotificationPreferencesDto dto) {
        UUID sellerId = getUserId();

        SellerNotificationPreferences notif = notificationRepository.findBySellerId(sellerId)
                .orElse(new SellerNotificationPreferences());

        notif.setOrderEmail(dto.orderEmail());
        notif.setOrderPush(dto.orderPush());
        notif.setOrderSms(dto.orderSms());
        notif.setPaymentEmail(dto.paymentEmail());
        notif.setPaymentPush(dto.paymentPush());
        notif.setPaymentSms(dto.paymentSms());
        notif.setStockEmail(dto.stockEmail());
        notif.setStockPush(dto.stockPush());
        notif.setStockSms(dto.stockSms());
        notif.setPromoEmail(dto.promoEmail());
        notif.setPromoPush(dto.promoPush());
        notif.setPromoSms(dto.promoSms());
        notif.setSecurityEmail(dto.securityEmail());
        notif.setSecurityPush(dto.securityPush());
        notif.setSecuritySms(dto.securitySms());
        notif.setSeller(entityManager.getReference(Seller.class, sellerId));

        notificationRepository.save(notif);
        return new ApiResponse<>(true, "Notification preferences updated", buildNotifInfo(notif), 200);
    }

    // ─────────────────────────────────────────────
    // SECURITY
    // ─────────────────────────────────────────────
    public ApiResponse<Object> changePassword(ChangePasswordDto dto) {
        // This app has no password-based authentication — sellers sign in via
        // phone + OTP only (see AuthService), and Seller has no password field.
        return new ApiResponse<>(false,
                "Password login is not supported. This account signs in with a phone OTP.", null, 400);
    }

    public ApiResponse<Object> updateSecurityQuestions(SecurityQuestionsDto dto) {
        // No password-based recovery flow exists to back security questions.
        return new ApiResponse<>(false,
                "Security questions are not supported. This account signs in with a phone OTP.", null, 400);
    }

    public ApiResponse<Object> getActiveSessions() {
        UUID sellerId = getUserId();
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(sellerId);

        List<Map<String, Object>> sessions = tokens.stream()
                .filter(t -> !t.isExpired())
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId());
                    map.put("createdAt", t.getCreatedAt());
                    map.put("expiresAt", t.getExpiresAt());
                    return (Map<String, Object>) map;
                })
                .collect(java.util.stream.Collectors.toList());

        return new ApiResponse<>(true, "Active sessions fetched", sessions, 200);
    }

    @Transactional
    public ApiResponse<Object> revokeSession(String sessionId) {
        UUID sellerId = getUserId();
        UUID tokenId;
        try {
            tokenId = UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Invalid session id", null, 400);
        }

        RefreshToken token = refreshTokenRepository.findByIdAndUserId(tokenId, sellerId)
                .orElse(null);
        if (token == null) {
            return new ApiResponse<>(false, "Session not found", null, 404);
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return new ApiResponse<>(true, "Session revoked", null, 200);
    }

    // ─────────────────────────────────────────────
    // PREFERENCES
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getPreferences() {
        UUID sellerId = getUserId();
        SellerPreferences prefs = preferencesRepository.findBySellerId(sellerId)
                .orElse(new SellerPreferences());
        return new ApiResponse<>(true, "Preferences fetched", buildPrefsInfo(prefs), 200);
    }

    @Transactional
    public ApiResponse<Object> updatePreferences(PreferencesDto dto) {
        UUID sellerId = getUserId();

        SellerPreferences prefs = preferencesRepository.findBySellerId(sellerId)
                .orElse(new SellerPreferences());

        prefs.setLanguage(dto.language());
        prefs.setTheme(dto.theme());
        prefs.setCurrency(dto.currency());
        prefs.setTimeZone(dto.timeZone());
        prefs.setSeller(entityManager.getReference(Seller.class, sellerId));

        preferencesRepository.save(prefs);
        return new ApiResponse<>(true, "Preferences updated", buildPrefsInfo(prefs), 200);
    }

    // ─────────────────────────────────────────────
    // Builder helpers (avoid exposing full entity)
    // ─────────────────────────────────────────────
    private Map<String, Object> buildPersonalInfo(Seller seller) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fullName", seller.getLegalName());
        map.put("displayName", seller.getDisplayName());
        map.put("email", seller.getEmail());
        map.put("emailVerified", seller.isEmailVerified());
        map.put("pendingEmail", seller.getPendingEmail());
        map.put("phone", seller.getPhone());
        map.put("profile_image", seller.getProfilePhotoUrl());
        map.put("media_files", seller.getProfileImageAndVideos());
        logger.info("media Files: " + seller.getProfileImageAndVideos());
        if (seller.getAddress() != null) {
            map.put("address", seller.getAddress().getLine1());
            map.put("city", seller.getAddress().getCity());
            map.put("state", seller.getAddress().getState());
            map.put("pinCode", seller.getAddress().getPincode());
        }
        return map;
    }

    private Map<String, Object> buildBusinessDetails(Seller seller) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("businessName", seller.getLegalName());
        map.put("businessType", seller.getBusinessType());
        map.put("gstNumber", seller.getGstNumber());
        map.put("businessCategory", seller.getCategory() != null ? seller.getCategory().getId() : null);
        return map;
    }

    private Map<String, Object> buildBankInfo(SellerBankDetails bank) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accountHolderName", bank.getAccountHolderName());
        map.put("accountNumber", maskAccountNumber(bank.getAccountNumber()));
        map.put("ifscCode", bank.getIfscCode());
        map.put("bankName", bank.getBankName());
        map.put("verified", bank.isVerified());
        return map;
    }

    private Map<String, Object> buildNotifInfo(SellerNotificationPreferences n) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderEmail", n.isOrderEmail());
        map.put("orderPush", n.isOrderPush());
        map.put("orderSms", n.isOrderSms());
        map.put("paymentEmail", n.isPaymentEmail());
        map.put("paymentPush", n.isPaymentPush());
        map.put("paymentSms", n.isPaymentSms());
        map.put("stockEmail", n.isStockEmail());
        map.put("stockPush", n.isStockPush());
        map.put("stockSms", n.isStockSms());
        map.put("promoEmail", n.isPromoEmail());
        map.put("promoPush", n.isPromoPush());
        map.put("promoSms", n.isPromoSms());
        map.put("securityEmail", n.isSecurityEmail());
        map.put("securityPush", n.isSecurityPush());
        map.put("securitySms", n.isSecuritySms());
        return map;
    }

    private Map<String, Object> buildPrefsInfo(SellerPreferences p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("language", p.getLanguage() != null ? p.getLanguage() : "English");
        map.put("theme", p.getTheme() != null ? p.getTheme() : "Light");
        map.put("currency", p.getCurrency() != null ? p.getCurrency() : "₹ INR (Indian Rupee)");
        map.put("timeZone", p.getTimeZone() != null ? p.getTimeZone() : "Asia/Kolkata (IST)");
        return map;
    }

    private Map<String, Object> defaultNotifications() {
        return Map.ofEntries(
                Map.entry("orderEmail", true),
                Map.entry("orderPush", true),
                Map.entry("orderSms", true),

                Map.entry("paymentEmail", true),
                Map.entry("paymentPush", true),
                Map.entry("paymentSms", false),

                Map.entry("stockEmail", true),
                Map.entry("stockPush", false),
                Map.entry("stockSms", false),

                Map.entry("promoEmail", true),
                Map.entry("promoPush", false),
                Map.entry("promoSms", false),

                Map.entry("securityEmail", true),
                Map.entry("securityPush", true),
                Map.entry("securitySms", true));
    }

    private Map<String, Object> defaultPreferences() {
        return Map.of(
                "language", "English",
                "theme", "Light",
                "currency", "₹ INR (Indian Rupee)",
                "timeZone", "Asia/Kolkata (IST)");
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4)
            return "****";
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}
// hjhuj gyhu hhujuhhhhhhhjgyy hgjyjgygyhjkj kuh uhkhuk ihuuhnjjkj kjj jbh
// gftrfrr