package com.ProductClientService.ProductClientService.DTO.reco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoResponseDto {
    /** Short-lived id echoed back in /reco/feedback so CTR can be attributed to a render. */
    private String recoId;
    private String userId;
    private String modelVersion;
    private String experimentVariant;
    private RecoContext context;
    private boolean coldStart;
    private List<RecoItemDto> items;
}
