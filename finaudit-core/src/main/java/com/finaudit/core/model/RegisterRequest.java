package com.finaudit.core.model;

public record RegisterRequest(
        String email,
        String password,
        UserRole role
) {}
