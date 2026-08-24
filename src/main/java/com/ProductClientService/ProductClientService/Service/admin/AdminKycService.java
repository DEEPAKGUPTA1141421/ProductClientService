package com.ProductClientService.ProductClientService.Service.admin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerKycDocument;
import com.ProductClientService.ProductClientService.Repository.SellerKycDocumentRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Service.SellerQrService;
import com.ProductClientService.ProductClientService.Service.ShopIndexer;
import com.ProductClientService.ProductClientService.Service.kafka.EventPublisherService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminKycService {

    private static final Logger logger = LoggerFactory.getLogger(AdminKycService.class);
    private static final String ZONE_KOLKATA = "Asia/Kolkata";

    private final SellerKycDocumentRepository kycRepository;
    private final SellerRepository sellerRepository;
    private final ShopIndexer shopIndexer;
    private final EventPublisherService eventPublisher;
    private final SellerQrService sellerQrService;

    public ApiResponse<Object> listPending(Pageable pageable) {
        Page<SellerKycDocument> page = kycRepository.findAllByStatus(SellerKycDocument.KycStatus.PENDING, pageable);
        List<Map<String, Object>> items = page.getContent().stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("totalElements", page.getTotalElements());
        data.put("totalPages", page.getTotalPages());
        return new ApiResponse<>(true, "Pending KYC submissions fetched", data, 200);
    }

    public ApiResponse<Object> getDetail(UUID sellerId) {
        SellerKycDocument kyc = kycRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("No KYC submission found for seller: " + sellerId));

        logger.info("Admin viewed full KYC detail for seller: {}", sellerId);

        Map<String, Object> map = buildSummary(kyc);
        // Full, decrypted values — admin-only view.
        map.put("aadhaarNumber", kyc.getAadhaarNumber());
        map.put("panNumber", kyc.getPanNumber());
        return new ApiResponse<>(true, "KYC detail fetched", map, 200);
    }

    @Transactional
    public ApiResponse<Object> approve(UUID sellerId, UUID adminId) {
        SellerKycDocument kyc = kycRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("No KYC submission found for seller: " + sellerId));
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found: " + sellerId));

        kyc.setStatus(SellerKycDocument.KycStatus.APPROVED);
        kyc.setRejectionReason(null);
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA)));
        kycRepository.save(kyc);
        if(!seller.getOnboardingStage().equals(Seller.ONBOARDSTAGE.BANK_ACCOUNT)){
            throw new RuntimeException("Pls Upload Banking Details, Kyc Documents and Banking Details Both get Validtaed Once");
        }
        seller.setOnboardingStage(Seller.ONBOARDSTAGE.DOCUMENT_VERIFIED);

        // Authorize the shop — same effect as AdminSellerController.activate().
        try {
            seller.setStatus("ACTIVE");
            String qrCodeUrl = sellerQrService.generateAndUploadSellerQr(seller);
            seller.setQrCodeUrl(qrCodeUrl);
        } catch (Exception e) {
            throw new RuntimeException("Seller QR generation failed: " + e.getMessage(), e);
        }
        sellerRepository.save(seller);

        shopIndexer.indexSeller(sellerId);
        eventPublisher.publishSellerLive(sellerId);

        logger.info("KYC approved and shop authorized for seller: {} by admin: {}", sellerId, adminId);
        return new ApiResponse<>(true, "KYC approved and shop authorized", buildSummary(kyc), 200);
    }

    @Transactional
    public ApiResponse<Object> reject(UUID sellerId, UUID adminId, String reason) {
        SellerKycDocument kyc = kycRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new RuntimeException("No KYC submission found for seller: " + sellerId));

        kyc.setStatus(SellerKycDocument.KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA)));
        kycRepository.save(kyc);

        logger.info("KYC rejected for seller: {} by admin: {} — reason: {}", sellerId, adminId, reason);
        return new ApiResponse<>(true, "KYC rejected", buildSummary(kyc), 200);
    }

    private Map<String, Object> buildSummary(SellerKycDocument kyc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sellerId", kyc.getSeller().getId());
        map.put("legalName", kyc.getSeller().getLegalName());
        map.put("phone", kyc.getSeller().getPhone());
        map.put("status", kyc.getStatus().name());
        map.put("aadhaarLast4", last4(kyc.getAadhaarNumber()));
        map.put("panLast4", last4(kyc.getPanNumber()));
        map.put("gstNumber", kyc.getGstNumber());
        map.put("aadhaarFrontUrl", kyc.getAadhaarFrontUrl());
        map.put("aadhaarBackUrl", kyc.getAadhaarBackUrl());
        map.put("panDocumentUrl", kyc.getPanDocumentUrl());
        map.put("gstDocumentUrl", kyc.getGstDocumentUrl());
        map.put("rejectionReason", kyc.getRejectionReason());
        map.put("submittedAt", kyc.getSubmittedAt());
        map.put("reviewedAt", kyc.getReviewedAt());
        return map;
    }

    private String last4(String value) {
        if (value == null || value.length() < 4)
            return null;
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
