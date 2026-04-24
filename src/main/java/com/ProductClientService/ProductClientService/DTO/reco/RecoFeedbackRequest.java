package com.ProductClientService.ProductClientService.DTO.reco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RecoFeedbackRequest {
    @NotBlank private String recoId;
    @NotNull  private UUID productId;
    /** CLICK | DISMISS | PURCHASE */
    @NotBlank private String action;
}
