package com.ProductClientService.ProductClientService.Controller;

import com.ProductClientService.ProductClientService.DTO.Cart.CouponDto;
import com.ProductClientService.ProductClientService.Service.cart.CouponService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping

    public ResponseEntity<?> create(@RequestBody @Valid CouponDto dto) {
        return ResponseEntity.ok(couponService.create(dto));
    }

    @GetMapping

    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @GetMapping("/{id}")

    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.getById(id));
    }

    @PutMapping("/{id}")

    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody CouponDto dto) {
        return ResponseEntity.ok(couponService.update(id, dto));
    }

    @DeleteMapping("/{id}")

    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return ResponseEntity.ok(couponService.delete(id));
    }
}

// huhu huuiui huuui njkjj jkj hukiui huiu hukuhju