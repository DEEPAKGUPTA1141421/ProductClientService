package com.ProductClientService.ProductClientService.Service.seller;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.Settings.BankDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.BusinessDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.ChangePasswordDto;
import com.ProductClientService.ProductClientService.DTO.Settings.NotificationPreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PersonalInfoDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.SecurityQuestionsDto;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Category;
import com.ProductClientService.ProductClientService.Model.SellerBankDetails;
import com.ProductClientService.ProductClientService.Model.SellerNotificationPreferences;
import com.ProductClientService.ProductClientService.Model.SellerPreferences;
import com.ProductClientService.ProductClientService.Repository.CategoryRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerBankDetailsRepository;
import com.ProductClientService.ProductClientService.Repository.SellerNotificationPreferencesRepository;
import com.ProductClientService.ProductClientService.Repository.SellerPreferencesRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService;
//import com.ProductClientService.ProductClientService.Repository.SellerSessionRepository;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;

@Service
@RequiredArgsConstructor
public class SellerSettingsService {

    private final SellerRepository sellerRepository;
    private final SellerBankDetailsRepository bankDetailsRepository;
    private final SellerNotificationPreferencesRepository notificationRepository;
    private final SellerPreferencesRepository preferencesRepository;
    private final CategoryRepository CategoryRepository;
    // private final SellerSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest request;
    private final EntityManager entityManager;
    private final GoogleMapsService googleMapsService;

    private final SellerAddressRepository sellerAddressRepository;

    // ─────────────────────────────────────────────
    // Helper: get current seller UUID from request
    // ─────────────────────────────────────────────
    private UUID getSellerId() {
        return (UUID) request.getAttribute("id");
    }

    private Seller getSellerOrThrow() {
        return sellerRepository.findById(getSellerId())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
    }

    // ─────────────────────────────────────────────
    // GET ALL SETTINGS (single call for frontend)
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getAllSettings() {
        UUID sellerId = getSellerId();

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

        seller.setLegalName(dto.fullName());
        seller.setDisplayName(dto.displayName());
        seller.setEmail(dto.email());
        seller.setPhone(dto.phone());

        // Update address if present
        if (seller.getAddress() != null) {
            AddressResponse addressDetails = googleMapsService.getAddressFromLatLng(dto.latitude(), dto.longitude());
            boolean isSaved = saveAddress(addressDetails, dto.phone(), dto.latitude(), dto.longitude());
        }

        // Profile photo upload handling (integrate with your file upload service)
        if (dto.profilePhoto() != null && !dto.profilePhoto().isEmpty()) {
            // TODO: upload to S3/local storage and set URL
            // String photoUrl = fileUploadService.upload(dto.profilePhoto());
            // seller.setProfilePhotoUrl(photoUrl);
        }

        sellerRepository.save(seller);
        return new ApiResponse<>(true, "Personal info updated", buildPersonalInfo(seller), 200);
    }

    private boolean saveAddress(AddressResponse addressDetails, String phone, BigDecimal lat, BigDecimal longi) {
        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
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

        seller.setLegalName(dto.businessName());
        Category category = CategoryRepository
                .findByIdAndCategoryLevel(dto.businessCategory(), Category.Level.SUPER_CATEGORY)
                .orElseThrow(() -> new RuntimeException("Invalid category"));
        seller.setCategory(category);
        // Store GST/PAN if your model has these fields
        // seller.setGstNumber(dto.gstNumber());
        // seller.setPanNumber(dto.panNumber());

        sellerRepository.save(seller);
        return new ApiResponse<>(true, "Business details updated", buildBusinessDetails(seller), 200);
    }

    // ─────────────────────────────────────────────
    // BANK DETAILS
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getBankDetails() {
        UUID sellerId = getSellerId();
        SellerBankDetails bank = bankDetailsRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("Bank details not found"));
        return new ApiResponse<>(true, "Bank details fetched", buildBankInfo(bank), 200);
    }

    @Transactional
    public ApiResponse<Object> updateBankDetails(BankDetailsDto dto) {
        UUID sellerId = getSellerId();

        SellerBankDetails bank = bankDetailsRepository.findBySellerId(sellerId)
                .orElse(new SellerBankDetails());

        bank.setAccountHolderName(dto.accountHolderName());
        bank.setAccountNumber(dto.accountNumber());
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
        UUID sellerId = getSellerId();
        SellerNotificationPreferences notif = notificationRepository.findBySellerId(sellerId)
                .orElse(new SellerNotificationPreferences());
        return new ApiResponse<>(true, "Notification preferences fetched", buildNotifInfo(notif), 200);
    }

    @Transactional
    public ApiResponse<Object> updateNotificationPreferences(NotificationPreferencesDto dto) {
        UUID sellerId = getSellerId();

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
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            return new ApiResponse<>(false, "Passwords do not match", null, 400);
        }

        Seller seller = getSellerOrThrow();

        // TODO: validate currentPassword against stored hash
        // if (!passwordEncoder.matches(dto.currentPassword(), seller.getPassword())) {
        // return new ApiResponse<>(false, "Incorrect current password", null, 400);
        // }

        // seller.setPassword(passwordEncoder.encode(dto.newPassword()));
        // sellerRepository.save(seller);

        return new ApiResponse<>(true, "Password updated successfully", null, 200);
    }

    public ApiResponse<Object> updateSecurityQuestions(SecurityQuestionsDto dto) {
        // TODO: Save to SellerSecurityQuestions entity
        return new ApiResponse<>(true, "Security questions updated", null, 200);
    }

    public ApiResponse<Object> getActiveSessions() {
        UUID sellerId = getSellerId();
        // TODO: fetch from SellerSession table
        List<Map<String, Object>> sessions = List.of(
                Map.of("id", "session-1", "device", "Chrome on Windows", "location", "Bangalore, IN", "time", "Now",
                        "current", true),
                Map.of("id", "session-2", "device", "Safari on iPhone", "location", "Bangalore, IN", "time",
                        "2 hours ago", "current", false));
        return new ApiResponse<>(true, "Active sessions fetched", sessions, 200);
    }

    public ApiResponse<Object> revokeSession(String sessionId) {
        // TODO: delete session from SellerSession table
        return new ApiResponse<>(true, "Session revoked", null, 200);
    }

    // ─────────────────────────────────────────────
    // PREFERENCES
    // ─────────────────────────────────────────────
    public ApiResponse<Object> getPreferences() {
        UUID sellerId = getSellerId();
        SellerPreferences prefs = preferencesRepository.findBySellerId(sellerId)
                .orElse(new SellerPreferences());
        return new ApiResponse<>(true, "Preferences fetched", buildPrefsInfo(prefs), 200);
    }

    @Transactional
    public ApiResponse<Object> updatePreferences(PreferencesDto dto) {
        UUID sellerId = getSellerId();

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
        map.put("phone", seller.getPhone());
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
        map.put("shopCategory", seller.getCategory() != null ? seller.getCategory().getId() : null);
        // map.put("gstNumber", seller.getGstNumber());
        // map.put("panNumber", seller.getPanNumber());
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
// hjhuj gyhu hhujuh