package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import com.ProductClientService.ProductClientService.Service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> submitReturn(
            @RequestPart("bookingId") String bookingId,
            @RequestPart("reason") String reason,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ReturnRequest.ReturnReason returnReason;
        try {
            returnReason = ReturnRequest.ReturnReason.valueOf(reason.toUpperCase());
        } catch (IllegalArgumentException e) {
            ApiResponse<Object> bad = new ApiResponse<>(false, "Invalid return reason: " + reason, null, 400);
            return ResponseEntity.badRequest().body(bad);
        }

        ApiResponse<Object> response = returnService.submitReturn(bookingId, returnReason, description, images);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getMyReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<Object> response = returnService.getMyReturns(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getReturnById(@PathVariable UUID id) {
        ApiResponse<Object> response = returnService.getReturnById(id);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}
