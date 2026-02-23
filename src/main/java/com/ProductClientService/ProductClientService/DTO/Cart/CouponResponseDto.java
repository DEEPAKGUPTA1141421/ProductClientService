package com.ProductClientService.ProductClientService.DTO.Cart;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Setter
@Getter
public class CouponResponseDto {

    @Getter
    @Setter
    @Builder
    public static class BestCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    @Setter
    @Getter
    @Builder
    public static class CashBackCoupon {
        private UUID id;
        private String code;
        private String leftParagraph;
        private String saveDescription;
        private String description;
    }

    @Setter
    @Getter
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
