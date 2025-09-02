package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ProductClientService.ProductClientService.Model.Section;

import java.util.List;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findByCategoryAndActiveTrueOrderByPositionAsc(String category);
}
