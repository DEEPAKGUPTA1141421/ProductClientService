package com.ProductClientService.ProductClientService.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.Banner;
import com.ProductClientService.ProductClientService.Service.BannerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Banner banner) {
        try {
            Banner savedBanner = bannerService.create(banner);
            ApiResponse<Object> response = new ApiResponse<>(true, "Banner created successfully", savedBanner, 200);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(false, e.getMessage(), null, 501);
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> getActiveBanners() {
        try {
            List<Banner> banners = bannerService.getActiveBanners();
            ApiResponse<Object> response = new ApiResponse<>(true, "Active banners fetched successfully", banners, 200);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(false, e.getMessage(), null, 501);
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        try {
            Banner banner = bannerService.getById(id);
            ApiResponse<Object> response = new ApiResponse<>(true, "Banner fetched successfully", banner, 200);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(false, e.getMessage(), null, 501);
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Banner updatedBanner) {
        try {
            Banner banner = bannerService.update(id, updatedBanner);
            ApiResponse<Object> response = new ApiResponse<>(true, "Banner updated successfully", banner, 200);
            return ResponseEntity.status(200).body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse<>(false, e.getMessage(), null, 501);
            System.out.println("Error: " + e.getMessage());
            return ResponseEntity.status(response.statusCode()).body(response);
        }
    }

}

// hbhyu hjgytggu gtuygutg