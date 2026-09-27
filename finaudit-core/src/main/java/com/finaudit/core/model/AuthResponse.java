package com.finaudit.core.model;

public record AuthResponse(
        String token,
        String refreshToken,
        Long id,
        String email,
        UserRole role,
        long expiresIn
) {}
