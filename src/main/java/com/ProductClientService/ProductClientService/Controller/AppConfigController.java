package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.UpsertConfigRequest;
import com.ProductClientService.ProductClientService.Service.AppConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Generic key → JSON config store. Any feature that needs a
 * backend-controlled list/object (dropdown options, share platforms, feature
 * toggles, etc.) can read it here instead of hardcoding it client-side.
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/{key}")
    public ApiResponse<Object> getByKey(@PathVariable String key) {
        return appConfigService.getByKey(key);
    }

    @PostMapping("/admin")
    public ApiResponse<Object> upsert(@Valid @RequestBody UpsertConfigRequest request) {
        return appConfigService.upsert(request.getKey(), request.getDescription(), request.getValue());
    }
}
