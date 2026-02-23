package com.ProductClientService.ProductClientService.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ProductClientService.ProductClientService.Model.SearchLog;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
}
