package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.reco.RecoContext;
import com.ProductClientService.ProductClientService.DTO.reco.RecoFeedbackRequest;
import com.ProductClientService.ProductClientService.DTO.reco.RecoResponseDto;
import com.ProductClientService.ProductClientService.Service.BaseService;
import com.ProductClientService.ProductClientService.Service.reco.RecoOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reco")
@RequiredArgsConstructor
public class RecoController extends BaseService {

    private final RecoOrchestrator orchestrator;

    /** GET /api/v1/reco/for-you?context=HOME|PDP|CART&k=20&sessionId=... */
    @GetMapping("/for-you")
    public ResponseEntity<ApiResponse<RecoResponseDto>> forYou(
            @RequestParam(name = "context", required = false) String context,
            @RequestParam(name = "k", defaultValue = "20") int k,
            @RequestParam(name = "sessionId", required = false) UUID sessionId) {

        UUID userId = getUserId();
        RecoResponseDto resp = orchestrator.forYou(userId, k, RecoContext.parse(context), sessionId);
        return ResponseEntity.ok(new ApiResponse<>(true, "ok", resp, 200));
    }

    /** POST /api/v1/reco/feedback  — attribute an action to a prior render. */
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Object>> feedback(@Valid @RequestBody RecoFeedbackRequest req) {
        UUID userId = getUserId();
        orchestrator.recordFeedback(req.getRecoId(), userId, req.getProductId(), req.getAction());
        return ResponseEntity.accepted().body(new ApiResponse<>(true, "accepted", null, 202));
    }
}
