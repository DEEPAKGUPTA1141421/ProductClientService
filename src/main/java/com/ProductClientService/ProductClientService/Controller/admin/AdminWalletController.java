package com.ProductClientService.ProductClientService.Controller.admin;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final WalletService walletService;

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<Object>> credit(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> body) {
        long amountPaise = ((Number) body.get("amountPaise")).longValue();
        String note = (String) body.get("note");
        return ResponseEntity.ok(walletService.adminCredit(userId, amountPaise, note));
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse<Object>> debit(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> body) {
        long amountPaise = ((Number) body.get("amountPaise")).longValue();
        String note = (String) body.get("note");
        ApiResponse<Object> response = walletService.adminDebit(userId, amountPaise, note);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
