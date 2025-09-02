package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.Banner;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {
    List<Banner> findByIsActiveTrueOrderByPriorityAsc();
}