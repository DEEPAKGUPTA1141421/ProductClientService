package com.ProductClientService.ProductClientService.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeIgnoreCaseAndActiveTrue(String code);

    List<Coupon> findByActiveTrue();

    List<Coupon> findByActiveTrueAndApplicabilityAndMinCartTotalLessThanEqual(
            Coupon.Applicability applicability,
            String cartAmount);
    List<Coupon> findByActiveTrueAndApplicabilityAndMinCartTotalGreaterThan(Coupon.Applicability applicability,
            String cartAmount);
}
