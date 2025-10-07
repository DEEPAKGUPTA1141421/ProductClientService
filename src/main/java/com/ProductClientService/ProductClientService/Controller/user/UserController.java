package com.ProductClientService.ProductClientService.Controller.user;

import org.springframework.web.bind.annotation.RestController;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;

import com.ProductClientService.ProductClientService.DTO.RecentSearchRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;

import com.ProductClientService.ProductClientService.Model.UserRecentSearch;
import com.ProductClientService.ProductClientService.Service.user.UserService;
import com.ProductClientService.ProductClientService.Utils.annotation.PrivateApi;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @GetMapping(value = "/get-user")
    @PrivateApi
    public ResponseEntity<?> getUser() {
        try {
            ApiResponse<Object> response = userService.getUser();
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

    @PutMapping("/set-default/{addressId}")
    @PrivateApi
    public ResponseEntity<ApiResponse<Object>> setDefaultAddress(@PathVariable UUID addressId) {
        try {
            ApiResponse<Object> response = userService.setDefaultAddress(addressId);
            return ResponseEntity
                    .status(response.statusCode())
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, e.getMessage(), null, 500));
        }
    }

    @PostMapping("/save")
    @PrivateApi
    public ResponseEntity<?> saveSearch(@RequestBody RecentSearchRequest request) {
        userService.saveSearch(request.itemId(), request.itemType(), request.title(),
                request.imageUrl(), request.meta());
        return ResponseEntity
                .status(200)
                .body(new ApiResponse<>(true, "Saved Successful", null, 200));
    }

    @GetMapping("/last")
    @PrivateApi
    public ResponseEntity<?> getLastSearches() {
        List<UserRecentSearch> searches = userService.getLastSearches();
        return ResponseEntity
                .status(200)
                .body(new ApiResponse<>(true, "Saved Successful", searches, 200));
    }
}
// uhiuhu uihiuh hjkj h8yiuhy uyg97 gfyugyugujnnnkjnn nkjnnkjn jihknk
// hhiuiuo9ujkhjbhjbhjb hbjbhjb bhuihiuhyiuyiuyiuyuiuyi
// jijuijiu joijioo jiuu9o8u9 iuui8u87yyu
// hiuhuo8uo90ih09iju98unkjhuhuihhuyubjbuguygu guytutuyt