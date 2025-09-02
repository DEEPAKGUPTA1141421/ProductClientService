package com.ProductClientService.ProductClientService.Model;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
@Entity
@Table(name = "otp")
public class Otp {
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
    private Date expiryTime = new Date(System.currentTimeMillis() + 5 * 60 * 1000); // 5 minutes from now
    
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
    public Date getExpiryTime() {
        return expiryTime;
    }
    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
    }

    public typeOfOtp getType() {
        return type;
    }

    public void setType(typeOfOtp type) {
        this.type = type;
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

    // Getters and setters (recommended for private fields)
    public ZonedDateTime getCreatedAt() {
        return createdAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
    }
    
    public enum typeOfOtp {
        login,
        registration,
        passwordReset,
        orderConfirmation,
        parcelDelivered,
        parcelCancelled,
        parcelReturned,
    }
}
