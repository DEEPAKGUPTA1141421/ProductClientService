package com.ProductClientService.ProductClientService.Controller.seller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Settings.BankDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.BusinessDetailsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.ChangePasswordDto;
import com.ProductClientService.ProductClientService.DTO.Settings.NotificationPreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PersonalInfoDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PreferencesDto;
import com.ProductClientService.ProductClientService.DTO.Settings.SecurityQuestionsDto;
import com.ProductClientService.ProductClientService.Service.seller.SellerSettingsService;

@RestController
@RequestMapping("/api/v1/seller/settings")
@RequiredArgsConstructor
public class SellerSettingsController {

    private final SellerSettingsService sellerSettingsService;

    // ─────────────────────────────────────────────
    // GET: Fetch all settings (profile, business, bank, notifications, preferences)
    // ─────────────────────────────────────────────
    @GetMapping("/all")

    public ResponseEntity<?> getAllSettings() {
        ApiResponse<Object> response = sellerSettingsService.getAllSettings();
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // PERSONAL INFO
    // ─────────────────────────────────────────────
    @GetMapping("/personal")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getPersonalInfo() {
        ApiResponse<Object> response = sellerSettingsService.getPersonalInfo();
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping(value = "/personal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> updatePersonalInfo(@Valid @ModelAttribute PersonalInfoDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updatePersonalInfo(dto);
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // BUSINESS DETAILS
    // ─────────────────────────────────────────────
    @GetMapping("/business")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getBusinessDetails() {
        ApiResponse<Object> response = sellerSettingsService.getBusinessDetails();
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/business")

    public ResponseEntity<?> updateBusinessDetails(@Valid @RequestBody BusinessDetailsDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updateBusinessDetails(dto);
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // BANK ACCOUNT
    // ─────────────────────────────────────────────
    @GetMapping("/bank")

    public ResponseEntity<?> getBankDetails() {
        ApiResponse<Object> response = sellerSettingsService.getBankDetails();
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/bank")

    public ResponseEntity<?> updateBankDetails(@Valid @RequestBody BankDetailsDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updateBankDetails(dto);
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // NOTIFICATIONS
    // ─────────────────────────────────────────────
    @GetMapping("/notifications")

    public ResponseEntity<?> getNotificationPreferences() {
        ApiResponse<Object> response = sellerSettingsService.getNotificationPreferences();
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/notifications")

    public ResponseEntity<?> updateNotificationPreferences(@Valid @RequestBody NotificationPreferencesDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updateNotificationPreferences(dto);
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // SECURITY
    // ─────────────────────────────────────────────
    @PutMapping("/security/password")

    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        ApiResponse<Object> response = sellerSettingsService.changePassword(dto);
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/security/questions")

    public ResponseEntity<?> updateSecurityQuestions(@Valid @RequestBody SecurityQuestionsDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updateSecurityQuestions(dto);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/security/sessions")

    public ResponseEntity<?> getActiveSessions() {
        ApiResponse<Object> response = sellerSettingsService.getActiveSessions();
        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping("/security/sessions/{sessionId}")

    public ResponseEntity<?> revokeSession(@PathVariable String sessionId) {
        ApiResponse<Object> response = sellerSettingsService.revokeSession(sessionId);
        return ResponseEntity.status(200).body(response);
    }

    // ─────────────────────────────────────────────
    // PREFERENCES
    // ─────────────────────────────────────────────
    @GetMapping("/preferences")

    public ResponseEntity<?> getPreferences() {
        ApiResponse<Object> response = sellerSettingsService.getPreferences();
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/preferences")

    public ResponseEntity<?> updatePreferences(@Valid @RequestBody PreferencesDto dto) {
        ApiResponse<Object> response = sellerSettingsService.updatePreferences(dto);
        return ResponseEntity.status(200).body(response);
    }
}
