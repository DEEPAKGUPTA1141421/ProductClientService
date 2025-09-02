package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ProductClientService.ProductClientService.Model.Otp;
import org.springframework.stereotype.Repository;
import java.util.Date;

@Repository
public interface OtpRepository extends JpaRepository <Otp, UUID>{
    default Otp CreateOtp(String phone, String type, String otp_code ) {
        Otp otp = new Otp();
        otp.setPhone(phone);
        otp.setOtpCode(otp_code);
        otp.setType(Otp.typeOfOtp.valueOf(type));
        return save(otp);
    }

    Otp findTopByPhoneAndTypeOrderByCreatedAtDesc(String phone, Otp.typeOfOtp type);
    default boolean checkOtpValidity(String phone, String otpCode, String type) {
        Otp latestOtp = findTopByPhoneAndTypeOrderByCreatedAtDesc(phone, Otp.typeOfOtp.valueOf(type));
        if (latestOtp == null) return false; 
        if (new Date().after(latestOtp.getExpiryTime())) return false;
        return latestOtp.getOtpCode().equals(otpCode);
    }
}
