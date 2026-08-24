package com.ProductClientService.ProductClientService.Service.seller;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Settings.AadhaarDocumentsDto;
import com.ProductClientService.ProductClientService.DTO.Settings.GstDocumentDto;
import com.ProductClientService.ProductClientService.DTO.Settings.PanDocumentDto;
import com.ProductClientService.ProductClientService.Model.Seller;
import com.ProductClientService.ProductClientService.Model.SellerKycDocument;
import com.ProductClientService.ProductClientService.Model.SellerKycDocument.KycStatus;
import com.ProductClientService.ProductClientService.Repository.SellerKycDocumentRepository;
import com.ProductClientService.ProductClientService.Repository.SellerRepository;
import com.ProductClientService.ProductClientService.Service.BaseService;
import com.ProductClientService.ProductClientService.Service.ImageUploadService;

import lombok.RequiredArgsConstructor;

/**
 * Lets a seller submit Aadhaar, PAN and GST documents one at a time — each
 * section is validated and stored independently. Aadhaar + PAN are mandatory;
 * once both are present the submission auto-advances to PENDING (queued for
 * admin review) and Aadhaar/PAN become read-only. GST stays editable up until
 * approval since it's optional and doesn't gate review.
 */
@Service
@RequiredArgsConstructor
public class SellerKycService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(SellerKycService.class);
    private static final String KYC_FOLDER = "kyc-documents";
    private static final String ZONE_KOLKATA = "Asia/Kolkata";

    private final SellerRepository sellerRepository;
    private final SellerKycDocumentRepository kycRepository;
    private final ImageUploadService fileUploadService;

    public ApiResponse<Object> getKycStatus() {
        UUID sellerId = getUserId();
        Optional<SellerKycDocument> kyc = kycRepository.findBySellerId(sellerId);
        return new ApiResponse<>(true, "KYC status fetched", buildKycInfo(kyc.orElse(null)), 200);
    }

    @Transactional
    public ApiResponse<Object> submitAadhaar(AadhaarDocumentsDto dto) {
        UUID sellerId = getUserId();
        SellerKycDocument kyc = getOrCreate(sellerId);

        if (kyc.getStatus() == KycStatus.PENDING || kyc.getStatus() == KycStatus.APPROVED) {
            return new ApiResponse<>(false, lockedMessage(kyc.getStatus()), null, 409);
        }

        try {
            kyc.setAadhaarFrontUrl(fileUploadService.uploadImage(dto.aadhaarFront(), KYC_FOLDER));
            kyc.setAadhaarBackUrl(fileUploadService.uploadImage(dto.aadhaarBack(), KYC_FOLDER));
        } catch (Exception e) {
            logger.error("Aadhaar upload failed for seller {}: {}", sellerId, e.getMessage());
            return new ApiResponse<>(false, "Failed to upload Aadhaar documents: " + e.getMessage(), null, 500);
        }
        kyc.setAadhaarNumber(dto.aadhaarNumber());

        return afterSectionSaved(kyc, "Aadhaar details saved");
    }

    @Transactional
    public ApiResponse<Object> submitPan(PanDocumentDto dto) {
        UUID sellerId = getUserId();
        SellerKycDocument kyc = getOrCreate(sellerId);

        if (kyc.getStatus() == KycStatus.PENDING || kyc.getStatus() == KycStatus.APPROVED) {
            return new ApiResponse<>(false, lockedMessage(kyc.getStatus()), null, 409);
        }

        try {
            kyc.setPanDocumentUrl(fileUploadService.uploadImage(dto.panDocument(), KYC_FOLDER));
        } catch (Exception e) {
            logger.error("PAN upload failed for seller {}: {}", sellerId, e.getMessage());
            return new ApiResponse<>(false, "Failed to upload PAN document: " + e.getMessage(), null, 500);
        }
        kyc.setPanNumber(dto.panNumber());

        return afterSectionSaved(kyc, "PAN details saved");
    }

    @Transactional
    public ApiResponse<Object> submitGst(GstDocumentDto dto) {
        UUID sellerId = getUserId();
        SellerKycDocument kyc = getOrCreate(sellerId);

        if (kyc.getStatus() == KycStatus.APPROVED) {
            return new ApiResponse<>(false, lockedMessage(kyc.getStatus()), null, 409);
        }

        try {
            kyc.setGstDocumentUrl(fileUploadService.uploadImage(dto.gstDocument(), KYC_FOLDER));
        } catch (Exception e) {
            logger.error("GST upload failed for seller {}: {}", sellerId, e.getMessage());
            return new ApiResponse<>(false, "Failed to upload GST document: " + e.getMessage(), null, 500);
        }
        kyc.setGstNumber(dto.gstNumber());
        kycRepository.save(kyc);

        logger.info("GST details saved for seller: {}", sellerId);
        return new ApiResponse<>(true, "GST details saved", buildKycInfo(kyc), 200);
    }

    private SellerKycDocument getOrCreate(UUID sellerId) {
        return kycRepository.findBySellerId(sellerId).orElseGet(() -> {
            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            SellerKycDocument kyc = new SellerKycDocument();
            kyc.setSeller(seller);
            kyc.setStatus(KycStatus.IN_PROGRESS);
            return kyc;
        });
    }

    /**
     * Persists the section just edited and, once both Aadhaar and PAN are
     * complete, auto-advances the whole submission to PENDING for admin review.
     * A REJECTED submission that gets a section re-saved returns to review the
     * same way once both mandatory sections are complete again.
     */
    private ApiResponse<Object> afterSectionSaved(SellerKycDocument kyc, String message) {
        UUID sellerId = kyc.getSeller().getId();
        boolean aadhaarComplete = kyc.getAadhaarNumber() != null
                && kyc.getAadhaarFrontUrl() != null && kyc.getAadhaarBackUrl() != null;
        boolean panComplete = kyc.getPanNumber() != null && kyc.getPanDocumentUrl() != null;

        if (aadhaarComplete && panComplete) {
            kyc.setStatus(KycStatus.PENDING);
            kyc.setRejectionReason(null);
            kyc.setSubmittedAt(ZonedDateTime.now(ZoneId.of(ZONE_KOLKATA)));
            kycRepository.save(kyc);

            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Seller not found"));
            seller.setOnboardingStage(Seller.ONBOARDSTAGE.DOCUMENT_VERIFICATION_PENDING);
            sellerRepository.save(seller);

            logger.info("KYC submission complete and queued for review, seller: {}", sellerId);
            return new ApiResponse<>(true, "All mandatory documents received — submitted for review",
                    buildKycInfo(kyc), 200);
        }

        kyc.setStatus(KycStatus.IN_PROGRESS);
        kycRepository.save(kyc);
        return new ApiResponse<>(true, message, buildKycInfo(kyc), 200);
    }

    private String lockedMessage(KycStatus status) {
        return "Your documents are already " + status.name().toLowerCase()
                + " and cannot be edited right now";
    }

    private Map<String, Object> buildKycInfo(SellerKycDocument kyc) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (kyc == null) {
            map.put("status", null);
            map.put("aadhaarLast4", null);
            map.put("panLast4", null);
            map.put("gstNumber", null);
            map.put("aadhaarFrontUrl", null);
            map.put("aadhaarBackUrl", null);
            map.put("panDocumentUrl", null);
            map.put("gstDocumentUrl", null);
            map.put("rejectionReason", null);
            return map;
        }
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
        return map;
    }

    private String last4(String value) {
        if (value == null || value.length() < 4)
            return null;
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
