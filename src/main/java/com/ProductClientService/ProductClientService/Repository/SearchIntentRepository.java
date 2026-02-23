package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.SearchIntent;

@Repository
public interface SearchIntentRepository extends JpaRepository<SearchIntent, UUID> {
    boolean existsByKeyword(String keyword);
}
