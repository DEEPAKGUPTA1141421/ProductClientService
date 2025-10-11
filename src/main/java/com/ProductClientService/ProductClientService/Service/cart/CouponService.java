package com.ProductClientService.ProductClientService.Service.cart;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.DTO.Cart.CouponDto;
import com.ProductClientService.ProductClientService.Model.Coupon;
import com.ProductClientService.ProductClientService.Repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepo;

    @Transactional
    public ApiResponse<Object> create(CouponDto dto) {
        Coupon coupon = Coupon.builder()
                .code(dto.code())
                .scope(dto.scope())
                .discountType(dto.discountType())
                .discountValue(dto.discountValue())
                .applicability(dto.applicability())
                .productId(dto.productId())
                .brandId(dto.brandId())
                .categoryId(dto.categoryId())
                .minCartTotal(dto.minCartTotal())
                .startsAt(dto.startsAt())
                .endsAt(dto.endsAt())
                .active(dto.active() != null ? dto.active() : true)
                .build();

        couponRepo.save(coupon);
        return new ApiResponse<>(true, "Coupon created successfully", coupon, 201);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getAll() {
        List<Coupon> coupons = couponRepo.findAll();
        return new ApiResponse<>(true, "All coupons fetched", coupons, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Object> getById(UUID id) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coupon not found"));
        return new ApiResponse<>(true, "Coupon fetched successfully", coupon, 200);
    }

    @Transactional
    public ApiResponse<Object> update(UUID id, CouponDto dto) {
        Coupon coupon = couponRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coupon not found"));

        coupon.setCode(dto.code());
        coupon.setScope(dto.scope());
        coupon.setDiscountType(dto.discountType());
        coupon.setDiscountValue(dto.discountValue());
        coupon.setApplicability(dto.applicability());
        coupon.setProductId(dto.productId());
        coupon.setBrandId(dto.brandId());
        coupon.setCategoryId(dto.categoryId());
        coupon.setMinCartTotal(dto.minCartTotal());
        coupon.setStartsAt(dto.startsAt());
        coupon.setEndsAt(dto.endsAt());
        coupon.setActive(dto.active());

        couponRepo.save(coupon);
        return new ApiResponse<>(true, "Coupon updated successfully", coupon, 200);
    }

    @Transactional
    public ApiResponse<Object> delete(UUID id) {
        if (!couponRepo.existsById(id)) {
            return new ApiResponse<>(false, "Coupon not found", null, 404);
        }
        couponRepo.deleteById(id);
        return new ApiResponse<>(true, "Coupon deleted successfully", null, 200);
    }
}
// juuiuiuiij hkuuiuibhkuhuuiuhjuhiuyui