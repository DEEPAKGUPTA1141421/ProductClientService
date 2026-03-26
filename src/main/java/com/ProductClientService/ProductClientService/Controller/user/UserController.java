package com.ProductClientService.ProductClientService.Controller.user;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;

import com.ProductClientService.ProductClientService.DTO.RecentSearchRequest;
import com.ProductClientService.ProductClientService.DTO.SellerBasicInfo;
import com.ProductClientService.ProductClientService.DTO.user.UpdateEmailRequest;
import com.ProductClientService.ProductClientService.DTO.user.UpdateProfileRequest;
import com.ProductClientService.ProductClientService.DTO.user.VerifyEmailOtpRequest;
import com.ProductClientService.ProductClientService.Model.UserRecentSearch;
import com.ProductClientService.ProductClientService.Service.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(userService.getFullProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest dto) {
        return ResponseEntity.ok(userService.updateProfile(dto));
    }

    @PatchMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateAvatar(file));
    }

    @PostMapping(value = "/update-address")
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

    // @GetMapping(value = "/searchplace/{keyword}")
    // public ResponseEntity<?> searchPlace(@PathVariable String keyword) {
    // try {
    // ApiResponse<Object> response = userService.searchPlace(keyword);
    // return ResponseEntity
    // .status(200)
    // .body(response);
    // } catch (Exception e) {
    // ApiResponse<Object> response = new ApiResponse(false, e.getMessage(), null,
    // 501);
    // System.out.println("messge" + e);
    // return ResponseEntity
    // .status(response.statusCode())
    // .body(response);
    // }
    // }

    @PutMapping("/set-default/{addressId}")

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

    public ResponseEntity<?> saveSearch(@RequestBody RecentSearchRequest request) {
        userService.saveSearch(request.itemId(), request.itemType(), request.title(),
                request.imageUrl(), request.meta());
        return ResponseEntity
                .status(200)
                .body(new ApiResponse<>(true, "Saved Successful", null, 200));
    }

    @GetMapping("/last")

    public ResponseEntity<?> getLastSearches() {
        List<UserRecentSearch> searches = userService.getLastSearches();
        return ResponseEntity
                .status(200)
                .body(new ApiResponse<>(true, "Saved Successful", searches, 200));
    }

    @PostMapping("/verify-email/request")
    public ResponseEntity<?> requestEmailUpdate(@Valid @RequestBody UpdateEmailRequest dto) {
        return ResponseEntity.ok(userService.requestEmailUpdate(dto));
    }

    @PostMapping("/verify-email/confirm")
    public ResponseEntity<?> confirmEmailUpdate(@Valid @RequestBody VerifyEmailOtpRequest dto) {
        return ResponseEntity.ok(userService.verifyEmailOtp(dto));
    }

    // ── Account deletion ─────────────────────────────────────────────────────

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        return ResponseEntity.ok(userService.requestAccountDeletion());
    }
}
// uhiuhu uihiuh hjkj h8yiuhy uyg97 gfyugyugujnnnkjnn nkjnnkjn jihknk
// hhiuiuo9ujkhjbhjbhjb hbjbhjb bhuihiuhyiuyiuyiuyuiuyi
// jijuijiu joijioo jiuu9o8u9 iuui8u87yyu
// hiuhuo8uo90ih09iju98unkjhuhuihhuyubjbuguygu guytutuyt