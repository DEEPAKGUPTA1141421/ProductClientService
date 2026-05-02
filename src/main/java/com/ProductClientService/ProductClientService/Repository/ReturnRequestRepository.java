package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Page<ReturnRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<ReturnRequest> findByBookingIdAndUserId(String bookingId, UUID userId);

    boolean existsByBookingIdAndUserId(String bookingId, UUID userId);
}
