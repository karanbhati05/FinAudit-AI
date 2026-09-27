package com.finaudit.api.service;

import com.finaudit.api.entity.User;
import com.finaudit.api.exception.UserAlreadyExistsException;
import com.finaudit.api.repository.UserRepository;
import com.finaudit.api.security.JwtService;
import com.finaudit.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("auditor@finaudit.ai", "hashed-password", UserRole.AUDITOR);
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("register() should encode password, persist user, and return JWT tokens")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("auditor@finaudit.ai", "secret123", UserRole.AUDITOR);

        when(userRepository.existsByEmail("auditor@finaudit.ai")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token-jwt");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token-jwt");
        when(jwtService.getJwtExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("access-token-jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-jwt");
        assertThat(response.email()).isEqualTo("auditor@finaudit.ai");
        assertThat(response.role()).isEqualTo(UserRole.AUDITOR);
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() should throw UserAlreadyExistsException when email is taken")
    void shouldThrowWhenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("auditor@finaudit.ai", "secret123", UserRole.AUDITOR);
        when(userRepository.existsByEmail("auditor@finaudit.ai")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("auditor@finaudit.ai");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() should verify password hash and issue tokens on success")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("auditor@finaudit.ai", "secret123");

        when(userRepository.findByEmail("auditor@finaudit.ai")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("access-token-jwt");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh-token-jwt");
        when(jwtService.getJwtExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("access-token-jwt");
        assertThat(response.email()).isEqualTo("auditor@finaudit.ai");
    }

    @Test
    @DisplayName("login() should throw BadCredentialsException on password mismatch")
    void shouldThrowBadCredentialsOnWrongPassword() {
        LoginRequest request = new LoginRequest("auditor@finaudit.ai", "wrong-password");

        when(userRepository.findByEmail("auditor@finaudit.ai")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("refreshToken() should issue new access token when refresh token is valid")
    void shouldRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtService.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-refresh-token")).thenReturn("auditor@finaudit.ai");
        when(userRepository.findByEmail("auditor@finaudit.ai")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("new-refresh-token");
        when(jwtService.getJwtExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.token()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }
}
