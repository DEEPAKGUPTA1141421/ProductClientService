package com.ProductClientService.ProductClientService.Repository;

import com.ProductClientService.ProductClientService.Model.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, UUID> {
    Optional<AppConfig> findByConfigKeyAndActiveTrue(String configKey);
}
