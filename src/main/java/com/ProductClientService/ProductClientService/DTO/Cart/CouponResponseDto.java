package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CouponResponseDto {
    class BestCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    class CashBackCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    class MoreCoupon {
        private UUID id;
        private String code;
        private String AddMoreDescription;
        private String subDescription;
        private String leftParagraph;
        private String description;
    }
}