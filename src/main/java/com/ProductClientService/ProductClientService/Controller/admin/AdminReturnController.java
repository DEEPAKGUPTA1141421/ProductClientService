package com.ProductClientService.ProductClientService.Controller.admin;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import com.ProductClientService.ProductClientService.Service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final ReturnService returnService;

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Object>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String adminNote = body != null ? body.get("adminNote") : null;
        ApiResponse<Object> response = returnService.adminApprove(id, adminNote);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Object>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String adminNote = body != null ? body.get("adminNote") : null;
        ApiResponse<Object> response = returnService.adminReject(id, adminNote);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        ReturnRequest.ReturnStatus newStatus;
        try {
            newStatus = ReturnRequest.ReturnStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            ApiResponse<Object> bad = new ApiResponse<>(false, "Invalid status: " + statusStr, null, 400);
            return ResponseEntity.badRequest().body(bad);
        }
        ApiResponse<Object> response = returnService.adminUpdateStatus(id, newStatus);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
