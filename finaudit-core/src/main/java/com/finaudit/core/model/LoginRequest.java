package com.finaudit.core.model;

public record LoginRequest(
        String email,
        String password
) {}
