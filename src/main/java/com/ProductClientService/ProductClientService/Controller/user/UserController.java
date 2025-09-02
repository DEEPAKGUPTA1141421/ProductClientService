package com.ProductClientService.ProductClientService.Controller.user;

import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.ProductDto;
import com.ProductClientService.ProductClientService.DTO.ProductElasticDto;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.seller.ProductAttributeDto;
import com.ProductClientService.ProductClientService.DTO.seller.ProductVariantsDto;
import com.ProductClientService.ProductClientService.Repository.ProductRepository;
import com.ProductClientService.ProductClientService.Service.S3Service;
import com.ProductClientService.ProductClientService.Service.seller.SellerService;
import com.ProductClientService.ProductClientService.Service.user.UserService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import co.elastic.clients.elasticsearch.core.InfoRequest;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping(value = "/update-address")
    @PrivateApi
    public ResponseEntity<?> updateAddress(@RequestBody SellerBasicInfo infoRequest) {
        try {
            ApiResponse<Object> response = userService.handleLocaton(infoRequest);
            return ResponseEntity
                    .status(200)
                    .body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null, 501);
            System.out.println("messge" + e);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        }
    }

    @GetMapping(value = "/searchplace/{keyword}")
    public ResponseEntity<?> searchPlace(@PathVariable String keyword) {
        try {
            ApiResponse<Object> response = userService.searchPlace(keyword);
            return ResponseEntity
                    .status(200)
                    .body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null, 501);
            System.out.println("messge" + e);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        }
    }

    @GetMapping(value = "/all-address")
    @PrivateApi
    public ResponseEntity<?> AllAddress() {
        try {
            ApiResponse<Object> response = userService.AllAddress();
            return ResponseEntity
                    .status(200)
                    .body(response);
        } catch (Exception e) {
            ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null, 501);
            System.out.println("messge" + e);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        }
    }
}
// uhiuhu uihiuh hjkj h8yiuhy uyg97 gfyugyugujnnnkjnn nkjnnkjn jihknk
// hjkhjbhjbhjb hbjbhjb
