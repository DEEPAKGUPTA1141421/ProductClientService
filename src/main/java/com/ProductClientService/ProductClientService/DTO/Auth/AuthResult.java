package com.ProductClientService.ProductClientService.DTO.Auth;

import java.util.UUID;

public record AuthResult(UUID entityId, String role, Object userPayload) {
}
