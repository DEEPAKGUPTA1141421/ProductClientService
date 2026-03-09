package com.ProductClientService.ProductClientService.DTO.Settings;

public record PreferencesDto(
        String language,
        String theme,
        String currency,
        String timeZone) {
}
