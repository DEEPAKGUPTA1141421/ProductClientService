package com.ProductClientService.ProductClientService.Service;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.ProductClientService.ProductClientService.Model.AppConfig;
import com.ProductClientService.ProductClientService.Repository.AppConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    public ApiResponse<Object> getByKey(String key) {
        return appConfigRepository.findByConfigKeyAndActiveTrue(key)
                .<ApiResponse<Object>>map(cfg -> new ApiResponse<>(true, "Config fetched", cfg.getValue(), 200))
                .orElseGet(() -> new ApiResponse<>(false, "Config not found", null, 404));
    }

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
