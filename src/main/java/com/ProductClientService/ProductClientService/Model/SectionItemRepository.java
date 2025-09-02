package com.ProductClientService.ProductClientService.Model;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ProductClientService.ProductClientService.Model.SectionItem;

import java.util.List;
import java.util.UUID;

public interface SectionItemRepository extends JpaRepository<SectionItem, UUID> {
    List<SectionItem> findBySectionId(UUID sectionId);
}