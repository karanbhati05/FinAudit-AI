package com.finaudit.api.security;

import com.finaudit.api.entity.User;
import com.finaudit.core.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-signing-verification");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);

        testUser = new User("auditor@finaudit.ai", "hashedpass", UserRole.AUDITOR);
        testUser.setId(42L);
    }

    @Test
    @DisplayName("generateToken and claims extraction should function accurately")
    void shouldGenerateAndExtractClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("auditor@finaudit.ai");
        assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.AUDITOR);
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("generateRefreshToken should generate valid refresh token")
    void shouldGenerateRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtService.validateToken(refreshToken)).isTrue();
        assertThat(jwtService.extractEmail(refreshToken)).isEqualTo("auditor@finaudit.ai");
    }

    @Test
    @DisplayName("validateToken should return false for tampered or invalid token")
    void shouldRejectInvalidToken() {
        assertThat(jwtService.validateToken("invalid.token.structure")).isFalse();
        assertThat(jwtService.validateToken("")).isFalse();
    }
}
