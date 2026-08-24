package com.ProductClientService.ProductClientService.Configuration;

import com.ProductClientService.ProductClientService.Model.AppConfig;
import com.ProductClientService.ProductClientService.Repository.AppConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps well-known, code-owned app_config rows in sync with their definition
 * here on every startup (always overwritten, not seed-once) so this doubles
 * as a self-healing migration if the stored shape ever drifts. This project
 * seeds data via versioned SQL files that aren't auto-applied (no Flyway
 * wired up; ddl-auto=update only handles schema), so config rows are synced
 * here instead, matching the corresponding V30 migration file for
 * documentation.
 */
@Component
@RequiredArgsConstructor
public class AppConfigSeeder implements CommandLineRunner {

    private final AppConfigRepository appConfigRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        upsertSeed(
                "social_share_platforms",
                "Ordered list of platforms shown on the seller app \"Share Product\" sheet.",
                socialSharePlatforms());
    }

    private void upsertSeed(String key, String description, Object value) {
        AppConfig cfg = appConfigRepository.findByConfigKeyAndActiveTrue(key).orElseGet(AppConfig::new);
        cfg.setConfigKey(key);
        cfg.setDescription(description);
        cfg.setValue(objectMapper.valueToTree(value));
        cfg.setActive(true);
        appConfigRepository.save(cfg);
    }

    private List<Map<String, Object>> socialSharePlatforms() {
        return List.of(
                platform("whatsapp", "WhatsApp", "#25D366", "whatsapp", 1, "deeplink",
                        "whatsapp://send?text={text}", "https://wa.me/?text={text}"),
                platform("telegram", "Telegram", "#29A9EA", "telegram", 2, "deeplink",
                        "https://t.me/share/url?url=&text={text}", null),
                platform("twitter", "X / Twitter", "#000000", "twitter", 3, "deeplink",
                        "https://twitter.com/intent/tweet?text={text}", null),
                platform("facebook", "Facebook", "#1877F2", "facebook", 4, "system_share", null, null),
                platform("instagram", "Instagram", "#D62976", "instagram", 5, "system_share", null, null),
                platform("sms", "Messages", "#34C759", "sms", 6, "deeplink", "sms:?body={text}", null),
                platform("email", "Email", "#448AFF", "email", 7, "deeplink",
                        "mailto:?subject={subject}&body={text}", null),
                platform("more", "More", "#222222", "more", 8, "system_share", null, null));
    }

    private Map<String, Object> platform(String key, String label, String color, String icon, int order,
            String action, String urlTemplate, String fallbackUrlTemplate) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("color", color);
        m.put("icon", icon);
        m.put("order", order);
        m.put("action", action);
        if (urlTemplate != null) m.put("urlTemplate", urlTemplate);
        if (fallbackUrlTemplate != null) m.put("fallbackUrlTemplate", fallbackUrlTemplate);
        return m;
    }
}
