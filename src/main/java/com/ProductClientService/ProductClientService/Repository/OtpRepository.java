package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ProductClientService.ProductClientService.Model.Otp;
import com.ProductClientService.ProductClientService.Model.Otp.typeOfOtp;

import feign.Param;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Repository;
import java.util.Date;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    default Otp CreateOtp(String phone, String type, String otp_code) {
        Otp otp = new Otp();
        otp.setPhone(phone);
        otp.setOtpCode(otp_code);
        otp.setType(Otp.typeOfOtp.valueOf(type));
        return save(otp);
    }

    Otp findTopByPhoneAndTypeOrderByCreatedAtDesc(String phone, Otp.typeOfOtp type);

    default boolean checkOtpValidity(String phone, String otpCode, String type) {
        Otp latestOtp = findTopByPhoneAndTypeOrderByCreatedAtDesc(phone, Otp.typeOfOtp.valueOf(type));
        if (latestOtp == null)
            return false;
        if (new Date().after(latestOtp.getExpiryTime()))
            return false;
        return latestOtp.getOtpCode().equals(otpCode);
    }

    @Modifying
    @Transactional
    @Query("UPDATE Otp o SET o.isVerified = true " +
            "WHERE o.phone = :phone AND o.otpCode = :otpCode AND o.type = :type")
    int markAsVerified(@Param("phone") String phone,
            @Param("otpCode") String otpCode,
            @Param("type") typeOfOtp type);
}

// hguiyu78y gyutu8t guytutyutuiujkhhji hkkhhk lniuhjuo hiuhijio