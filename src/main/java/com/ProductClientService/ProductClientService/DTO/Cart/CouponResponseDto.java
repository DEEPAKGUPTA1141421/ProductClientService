package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
public class CouponResponseDto {

    @Data
    @Builder
    public static class BestCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    @Data
    @Builder
    public static class CashBackCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    @Data
    @Builder
    public static class MoreCoupon {
        private UUID id;
        private String code;
        private String addMoreDescription;
        private String subDescription;
        private String leftParagraph;
        private String description;
    }
}


