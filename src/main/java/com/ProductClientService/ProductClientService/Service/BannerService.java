package com.ProductClientService.ProductClientService.Service;

import org.springframework.stereotype.Service;

import com.ProductClientService.ProductClientService.Model.Banner;
import com.ProductClientService.ProductClientService.Repository.BannerRepository;

import java.util.List;
import java.util.UUID;

@Service
public class BannerService {

    private final BannerRepository bannerRepository;

    public BannerService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    public Banner create(Banner banner) {
        return bannerRepository.save(banner);
    }

    public List<Banner> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByPriorityAsc();
    }

    public Banner getById(UUID id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));
    }

    public Banner update(UUID id, Banner updatedBanner) {
        Banner existingBanner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        // Update only fields that may change
        existingBanner.setTitle(updatedBanner.getTitle());
        existingBanner.setDescription(updatedBanner.getDescription());
        existingBanner.setMediaType(updatedBanner.getMediaType());
        existingBanner.setMediaUrl(updatedBanner.getMediaUrl());
        existingBanner.setLogoUrl(updatedBanner.getLogoUrl());
        existingBanner.setBannerType(updatedBanner.getBannerType());
        existingBanner.setRedirectType(updatedBanner.getRedirectType());
        existingBanner.setRedirectRefId(updatedBanner.getRedirectRefId());
        existingBanner.setMetadata(updatedBanner.getMetadata());
        existingBanner.setStartDate(updatedBanner.getStartDate());
        existingBanner.setEndDate(updatedBanner.getEndDate());
        existingBanner.setIsActive(updatedBanner.getIsActive());
        existingBanner.setPriority(updatedBanner.getPriority());

        return bannerRepository.save(existingBanner);
    }
}