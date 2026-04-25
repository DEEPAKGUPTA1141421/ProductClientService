package com.ProductClientService.ProductClientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ProductClientService.ProductClientService.Model.Section;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findByCategoryAndActiveTrueOrderByPositionAsc(String category);

    @Query("SELECT DISTINCT s FROM Section s " +
            "LEFT JOIN FETCH s.items si " +
            "WHERE s.category = :category AND s.active = true " +
            "ORDER BY s.position ASC")
    List<Section> findActiveSectionsByCategory(@Param("category") String category);

    @Query("SELECT DISTINCT s FROM Section s " +
            "LEFT JOIN FETCH s.items si " +
            "WHERE s.category = :category AND s.active = true " +
            "AND (s.startsAt IS NULL OR s.startsAt <= :now) " +
            "AND (s.endsAt IS NULL OR s.endsAt > :now) " +
            "ORDER BY s.position ASC")
    List<Section> findPageSections(@Param("category") String category, @Param("now") OffsetDateTime now);
}

// hhuuiujhiuh hbgjgjy