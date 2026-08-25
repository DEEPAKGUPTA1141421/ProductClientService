package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.Configuration.CacheConfig;
import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.AppConfig;
import com.ProductClientService.ProductClientService.Repository.AppConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    public ApiResponse<Object> getByKey(String key) {
        Object value = getConfigValue(key);
        return value != null
                ? new ApiResponse<>(true, "Config fetched", value, 200)
                : new ApiResponse<>(false, "Config not found", null, 404);
    }

    /**
     * Raw config value for a key, cached in Redis so hot paths (e.g. get-cart)
     * can embed it without hitting the DB on every request. Null on miss —
     * caching is skipped for nulls (see {@link CacheConfig}).
     */
    @Cacheable(value = CacheConfig.APP_CONFIG, key = "#key")
    public Object getConfigValue(String key) {
        return appConfigRepository.findByConfigKeyAndActiveTrue(key)
                .map(cfg -> (Object) objectMapper.convertValue(cfg.getValue(), Object.class))
                .orElse(null);
    }

    @CacheEvict(value = CacheConfig.APP_CONFIG, key = "#key")
    public ApiResponse<Object> upsert(String key, String description, Object value) {
        AppConfig cfg = appConfigRepository.findByConfigKeyAndActiveTrue(key).orElseGet(AppConfig::new);
        cfg.setConfigKey(key);
        cfg.setDescription(description);
        cfg.setValue(objectMapper.valueToTree(value));
        cfg.setActive(true);
        AppConfig saved = appConfigRepository.save(cfg);
        return new ApiResponse<>(true, "Config saved", saved, 200);
    }
}
