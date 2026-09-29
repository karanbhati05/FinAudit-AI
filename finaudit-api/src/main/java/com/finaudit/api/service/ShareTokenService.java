package com.finaudit.api.service;

import com.finaudit.api.exception.ShareLinkExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class ShareTokenService {

    private static final Logger log = LoggerFactory.getLogger(ShareTokenService.class);

    private final String jwtSecret;
    private final long defaultShareValidityMs;

    public ShareTokenService(
            @Value("${finaudit.security.jwt.secret:default-secret-key-finaudit-ai-financial-auditor-must-be-at-least-256-bits-long}") String jwtSecret,
            @Value("${finaudit.share.validity-days:7}") int validityDays
    ) {
        this.jwtSecret = jwtSecret;
        this.defaultShareValidityMs = Duration.ofDays(validityDays).toMillis();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateShareToken(Long reportId) {
        return generateShareToken(reportId, defaultShareValidityMs);
    }

    public String generateShareToken(Long reportId, long validityMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(reportId))
                .claim("scope", "PUBLIC_SHARE")
                .claim("reportId", reportId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + validityMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Long validateAndExtractReportId(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Share token must not be empty.");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String scope = (String) claims.get("scope");
            if (!"PUBLIC_SHARE".equals(scope)) {
                throw new IllegalArgumentException("Invalid token scope for public report access.");
            }

            Object reportIdObj = claims.get("reportId");
            if (reportIdObj instanceof Number num) {
                return num.longValue();
            } else if (reportIdObj instanceof String str) {
                return Long.parseLong(str);
            }
            throw new IllegalArgumentException("Token does not contain a valid reportId claim.");
        } catch (ExpiredJwtException e) {
            log.warn("Public share token has expired: {}", e.getMessage());
            throw new ShareLinkExpiredException("This public share link has expired. Please request a new link from the report auditor.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid public share token: {}", e.getMessage());
            throw new IllegalArgumentException("Malformed or invalid share link: " + e.getMessage());
        }
    }
}
