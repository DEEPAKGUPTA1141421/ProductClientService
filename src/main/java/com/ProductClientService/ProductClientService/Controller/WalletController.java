package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getBalance() {
        return ResponseEntity.ok(walletService.getBalance());
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Object>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(walletService.getTransactions(page, size));
    }

    @PostMapping("/pay/{bookingId}")
    public ResponseEntity<ApiResponse<Object>> pay(
            @PathVariable String bookingId,
            @RequestBody Map<String, Long> body) {
        Long amountPaise = body.get("amountPaise");
        if (amountPaise == null || amountPaise <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "amountPaise is required and must be > 0", null, 400));
        }
        ApiResponse<Object> response = walletService.payWithWallet(bookingId, amountPaise);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
