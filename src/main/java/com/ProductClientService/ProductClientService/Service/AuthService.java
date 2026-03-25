package com.ProductClientService.ProductClientService.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.AuthRequest;
import com.ProductClientService.ProductClientService.DTO.LoginRequest;
import com.ProductClientService.ProductClientService.DTO.NotificationRequest;
import com.ProductClientService.ProductClientService.DTO.RefreshRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.TokenPairResponse;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.CreateRiderDto;
import com.ProductClientService.ProductClientService.DTO.network.DeliveryInvetoryApiDto.RiderIdResponse;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.Seller.ONBOARDSTAGE;
import com.ProductClientService.ProductClientService.Model.User;
import com.ProductClientService.ProductClientService.Model.Otp.typeOfOtp;
import com.ProductClientService.ProductClientService.Model.RefreshToken;
import com.ProductClientService.ProductClientService.Repository.OtpRepository;
import com.ProductClientService.ProductClientService.Repository.RefreshTokenRepository;
import com.ProductClientService.ProductClientService.Repository.SellerAddressRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Repository.UserRepojectory;
import com.ProductClientService.ProductClientService.Service.GoogleMapsService.AddressResponse;
import com.ProductClientService.ProductClientService.Utils.RateLimiter;
import com.ProductClientService.ProductClientService.network.DeliveryInventoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.imageio.ImageIO;
import java.awt.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final Cloudinary cloudinary;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public ApiResponse<?> login(LoginRequest loginRequest) {

        try {

            boolean isSignup = false;

            if (loginRequest.typeOfUser() == LoginRequest.UserType.SELLER) {

                Optional<Seller> seller = sellerRepository.findByPhone(loginRequest.phone());

                if (seller.isEmpty()) {
                    Seller newSeller = new Seller();
                    newSeller.setPhone(loginRequest.phone());
                    sellerRepository.save(newSeller);
                    isSignup = true;
                }

            } else if (loginRequest.typeOfUser() == LoginRequest.UserType.USER) {

                Optional<User> user = userRepojectory.findByPhone(loginRequest.phone());

                if (user.isEmpty()) {
                    User newUser = new User();
                    newUser.setPhone(loginRequest.phone());
                    userRepojectory.save(newUser);
                    isSignup = true;
                }

            } else if (loginRequest.typeOfUser() == LoginRequest.UserType.RIDER) {

                // Rider logic

            } else {
                return new ApiResponse<>(false, "Invalid User Type", null, 403);
            }

            sendOtpAsync(loginRequest.phone(), "login");
            Map<String, String> logData = new HashMap<>();
            logData.put("phone", loginRequest.phone());
            logData.put("isSignup", String.valueOf(isSignup));
            logger.info("Login attempt: {}", logData);
            return new ApiResponse<>(true, "OTP sent", logData, 200);
        } catch (Exception e) {
            System.out.println("authservice login method error " + e);
            return new ApiResponse<>(false, e.getMessage(), null, 500);
        }
    }

    public ApiResponse<?> verify(AuthRequest authrequest) {

        boolean valid = otpRepository.checkOtpValidity(
                authrequest.phone(),
                authrequest.otp_code(),
                "login");

        if (!valid) {
            return new ApiResponse<>(false, "Otp is Invalid", null, 200);
        }

        otpRepository.markAsVerified(
                authrequest.phone(),
                authrequest.otp_code(),
                typeOfOtp.login);

        if (authrequest.typeOfUser() == AuthRequest.UserType.SELLER) {

            Seller seller = sellerRepository.findByPhone(authrequest.phone())
                    .orElseThrow(() -> new RuntimeException("Seller not found"));

            String accessToken = jwtService.generateToken(authrequest.phone(), "SELLER", seller.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    seller.getId(), authrequest.phone(), "SELLER", null);

            return new ApiResponse<>(true, "OTP Verification Success",
                    new TokenPairResponse(
                            accessToken,
                            refreshToken.getToken(),
                            jwtService.getExpirationInSeconds(), // add this getter to JwtService
                            refreshToken.getExpiresAt().toEpochSecond() -
                                    java.time.ZonedDateTime.now().toEpochSecond(),
                            seller),
                    200);

        } else if (authrequest.typeOfUser() == AuthRequest.UserType.USER) {

            User user = userRepojectory.findByPhone(authrequest.phone())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String accessToken = jwtService.generateToken(authrequest.phone(), "USER", user.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user.getId(), authrequest.phone(), "USER", null);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("phone", user.getPhone());
            userData.put("name", user.getName());
            userData.put("status", user.getStatus());
            return new ApiResponse<>(true, "OTP Verification Success",
                    new TokenPairResponse(
                            accessToken,
                            refreshToken.getToken(),
                            jwtService.getExpirationInSeconds(),
                            refreshToken.getExpiresAt().toEpochSecond() -
                                    java.time.ZonedDateTime.now().toEpochSecond(),
                            userData),
                    200);
        } else if (authrequest.typeOfUser() == AuthRequest.UserType.RIDER) {

            ApiResponse<RiderIdResponse> response = deliveryInventoryClient.createRiderWithPhone(
                    new CreateRiderDto("PHONE", authrequest.phone()));

            if (response.statusCode() == 200 && response.success()) {
                UUID riderId = response.data().id();
                String accessToken = jwtService.generateToken(authrequest.phone(), "RIDER", riderId);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                        riderId, authrequest.phone(), "RIDER", null);

                return new ApiResponse<>(true, "OTP Verification Success",
                        new TokenPairResponse(
                                accessToken,
                                refreshToken.getToken(),
                                jwtService.getExpirationInSeconds(),
                                refreshToken.getExpiresAt().toEpochSecond() -
                                        java.time.ZonedDateTime.now().toEpochSecond(),
                                response.data()),
                        200);
            } else {
                throw new RuntimeException("Failed to create rider: " + response.message());
            }
        }

        return new ApiResponse<>(false, "Invalid User Type", null, 403);
    }

    public ApiResponse<?> refresh(RefreshRequest request) {
        try {
            RefreshToken oldToken = refreshTokenService.validateAndRotate(request.refreshToken());

            String newAccessToken = jwtService.generateToken(
                    oldToken.getPhone(),
                    oldToken.getUserType(),
                    oldToken.getUserId());

            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                    oldToken.getUserId(),
                    oldToken.getPhone(),
                    oldToken.getUserType(),
                    oldToken.getFamily()); // ← same family = rotation, not new session

            return new ApiResponse<>(true, "Token refreshed",
                    new TokenPairResponse(
                            newAccessToken,
                            newRefreshToken.getToken(),
                            jwtService.getExpirationInSeconds(),
                            newRefreshToken.getExpiresAt().toEpochSecond() -
                                    java.time.ZonedDateTime.now().toEpochSecond(),
                            null),
                    200);

        } catch (SecurityException e) {
            return new ApiResponse<>(false, e.getMessage(), null, 401);
        } catch (RuntimeException e) {
            return new ApiResponse<>(false, e.getMessage(), null, 401);
        }
    }

    public ApiResponse<?> logout(RefreshRequest request) {
        try {
            RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                    .orElseThrow(() -> new RuntimeException("Token not found"));
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            return new ApiResponse<>(true, "Logged out successfully", null, 200);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null, 400);
        }
    }

    public ApiResponse<?> logoutAll(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        return new ApiResponse<>(true, "All sessions revoked", null, 200);
    }

    @Async
    public void sendOtpAsync(String phone, String type) {
        String otpCode = generateOtp(); // Implement your OTP generation logic
        otpRepository.CreateOtp(phone, type, otpCode);
        logger.info("OTP created asynchronously for phone: {} and Otp: {} ", phone, otpCode);
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

    private ApiResponse<Seller> handleAdhadharCard(SellerBasicInfo inforequest) {
        String phone = (String) request.getAttribute("phone");
        // if (!sellerRepository.stageValidation(Seller.ONBOARDSTAGE.LOCATION, phone)) {
        // return new ApiResponse<>(false, "Stage is Not Correct", null, 403);
        // }

        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return new ApiResponse<>(true, "Invalid Phone", null, 200);
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.ADHADHAR_CARD);

        sellerAddressRepository.saveOrUpdateAadharAddress(seller, inforequest.adhadhar_card());
        return new ApiResponse<>(true, "Adhadhar Verification Complete", null, 200);
    }

    private ApiResponse<Seller> handlePanCard(SellerBasicInfo inforequest) throws WriterException, IOException {
        String phone = (String) request.getAttribute("phone");
        // if (!sellerRepository.stageValidation(Seller.ONBOARDSTAGE.ADHADHAR_CARD,
        // phone)) {
        // return new ApiResponse<>(false, "Stage is Not Correct", null, 403);
        // }

        Optional<Seller> optionalSeller = sellerRepository.findByPhone(phone);
        if (optionalSeller.isEmpty()) {
            return new ApiResponse<>(true, "Invalid Phone", null, 200);
        }

        Seller seller = optionalSeller.get();
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.DOCUMENT_VERIFIED);

        sellerAddressRepository.saveOrUpdatePanAddress(seller, inforequest.pan_card());
        // QR code text → redirect link

        // Generate QR and upload
        String qrUrl = generateAndUpload(seller.getId(), "src/main/resources/static/logo.png", 400, 400);
        System.out.println("qrText" + qrUrl);
        seller.setQrCodeUrl(qrUrl);
        sellerRepository.save(seller);
        return new ApiResponse<>(true, "PanCard Verification Complete", null, 200);
    }

    public String generateAndUpload(UUID id, String logoPath, int width, int height)
            throws WriterException, IOException {
        // 1. Setup error correction
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        // 2. Generate QR code matrix
        BitMatrix bitMatrix = new MultiFormatWriter().encode(id.toString(), BarcodeFormat.QR_CODE, width, height,
                hints);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // 3. Load logo
        BufferedImage logo = ImageIO.read(new File(logoPath));

        // Scale logo to fit inside QR (max ~20%)
        int logoWidth = qrImage.getWidth() / 5;
        int logoHeight = qrImage.getHeight() / 5;
        Image scaledLogo = logo.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

        // 4. Overlay logo at center
        Graphics2D g = qrImage.createGraphics();
        int x = (qrImage.getWidth() - logoWidth) / 2;
        int y = (qrImage.getHeight() - logoHeight) / 2;
        g.drawImage(scaledLogo, x, y, null);
        g.dispose();

        // 5. Write QR to byte stream
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", os);

        // 6. Upload to Cloudinary
        Map uploadResult = cloudinary.uploader().upload(os.toByteArray(), ObjectUtils.emptyMap());
        return uploadResult.get("url").toString();
    }
}

// huyh hihi hyihi hyh huih huihu huj ggygggygggggg
// kjj juji hukjiij hjuhhuughuhuhuhhh
