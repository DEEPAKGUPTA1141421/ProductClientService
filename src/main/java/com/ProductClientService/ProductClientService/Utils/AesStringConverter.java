package com.ProductClientService.ProductClientService.Utils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * AES/GCM converter for JPA columns storing sensitive PII (Aadhaar/PAN
 * numbers, bank account numbers). The key comes from kyc.encryption.key (a
 * base64-encoded 32-byte value, env-backed via KYC_ENCRYPTION_KEY) so it
 * never lives in source.
 *
 * Decryption falls back to returning the stored value as-is when it isn't
 * valid ciphertext, so pre-existing plaintext rows (written before this
 * converter was applied to a column) keep working — they're re-encrypted
 * automatically the next time that row is saved.
 */
@Component
@Converter
public class AesStringConverter implements AttributeConverter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(AesStringConverter.class);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Value("${kyc.encryption.key}")
    private String base64Key;

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Failed to encrypt KYC field", e);
            throw new IllegalStateException("Failed to encrypt KYC field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);
            if (combined.length <= GCM_IV_LENGTH) {
                return storedValue; // too short to be our ciphertext — legacy plaintext
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Not valid ciphertext for our key/format — treat as a legacy
            // plaintext value written before encryption was applied.
            logger.warn("Value is not valid AES ciphertext, returning as-is (legacy plaintext?): {}", e.getMessage());
            return storedValue;
        }
    }

    private SecretKeySpec secretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
