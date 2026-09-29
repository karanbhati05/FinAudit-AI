package com.finaudit.api.service;

import com.finaudit.api.entity.User;
import com.finaudit.api.exception.UserAlreadyExistsException;
import com.finaudit.api.repository.UserRepository;
import com.finaudit.api.security.JwtService;
import com.finaudit.core.model.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("A user with email '" + request.email() + "' already exists.");
        }

        UserRole role = request.role() != null ? request.role() : UserRole.AUDITOR;
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(request.email(), encodedPassword, role);
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtService.getJwtExpirationMs()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtService.getJwtExpirationMs()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request.refreshToken() == null || !jwtService.validateToken(request.refreshToken())) {
            throw new BadCredentialsException("Invalid or expired refresh token.");
        }

        String email = jwtService.extractEmail(request.refreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found for token."));

        String newToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                newToken,
                newRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtService.getJwtExpirationMs()
        );
    }

    @Transactional
    public AuthResponse demoLogin() {
        User user = userRepository.findByEmail("demo@finaudit.ai")
                .orElseGet(() -> {
                    User demoUser = new User("demo@finaudit.ai", passwordEncoder.encode("DemoAuditor2026!"), UserRole.AUDITOR);
                    return userRepository.save(demoUser);
                });

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                token,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                jwtService.getJwtExpirationMs()
        );
    }
}
