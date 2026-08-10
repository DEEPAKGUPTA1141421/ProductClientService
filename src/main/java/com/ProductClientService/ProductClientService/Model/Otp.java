package com.ProductClientService.ProductClientService.Model;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "otp")
public class Otp {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ZONE_KOLKATA = "Asia/Kolkata";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone", nullable = false)
    private String phone;
    @Column(name = "otp_code", nullable = false)
    private String otpCode;
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;
    @Column(name = "expiry_time", nullable = false)
    private ZonedDateTime expiryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private typeOfOtp type;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public ZonedDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(ZonedDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public typeOfOtp getType() {
        return type;
    }

    public void setType(typeOfOtp type) {
        this.type = type;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime
            .now(ZoneId.of(ZONE_KOLKATA));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA));

    public ZonedDateTime getCreatedAt() {
        return createdAt.withZoneSameInstant(ZoneId.of(ZONE_KOLKATA));
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt.withZoneSameInstant(ZoneId.of(ZONE_KOLKATA));
    }

    public enum typeOfOtp {
        login,
        registration,
        passwordReset,
        orderConfirmation,
        parcelDelivered,
        parcelCancelled,
        parcelReturned,
        aadhaarVerification,
        emailVerification,
        emailUpdate
    }

    public Otp() {
    }

    public static Otp create(String phone, typeOfOtp type, boolean testOtpMode) {
        Otp otp = new Otp();
        otp.setPhone(phone);
        otp.setType(type);
        otp.setVerified(false);
        otp.setOtpCode(generateCode(testOtpMode));
        otp.setExpiryTime(ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA)).plusMinutes(5));
        return otp;
    }

    private static String generateCode(boolean testOtpMode) {
        if (testOtpMode) {
            return "123456";
        }
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}