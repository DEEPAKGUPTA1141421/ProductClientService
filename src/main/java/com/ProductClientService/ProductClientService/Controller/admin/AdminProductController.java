package com.ProductClientService.ProductClientService.Controller.admin;

import org.springframework.web.bind.annotation.RestController;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.admin.AttributeDto;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryAttributeRequest;
import com.ProductClientService.ProductClientService.DTO.admin.CategoryDto;
import com.ProductClientService.ProductClientService.Model.CategoryAttribute;
import com.ProductClientService.ProductClientService.Service.admin.AdminProductService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/product")
public class AdminProductController {
    @Autowired
    private AdminProductService adminProductService;

    @PostMapping("/add-category")
    @PrivateApi
    public ResponseEntity<?> addCategory(@Valid @RequestBody CategoryDto productrequest) {
        ApiResponse<Object> response = adminProductService.addCategory(productrequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/add-attribute1")
    public ResponseEntity<?> addAttribute(@Valid @RequestBody AttributeDto attributerequest) {
        System.out.println("Dto Matched");
        ApiResponse<Object> response = adminProductService.addAttribute(attributerequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PutMapping("/update-attribute/{id}")
    @PrivateApi
    public ResponseEntity<?> updateAttribute(@PathVariable UUID id, @Valid @RequestBody AttributeDto attributerequest) {
        ApiResponse<Object> response = adminProductService.updateAttributefun(id, attributerequest);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @GetMapping("/add-category-from-file")
    @PrivateApi
    public ResponseEntity<?> test() throws IOException {
        adminProductService.addCategoryFromJsonFile();
        return ResponseEntity.status(200).body("task Queued");
    }

}
// fytdfugyuguhi yguyduhjbj jugguyge gyuguyduygjgyhguyjd gygd giyiudbhjgdiu
// njhuikd kj.hikd, mkjhkuh 