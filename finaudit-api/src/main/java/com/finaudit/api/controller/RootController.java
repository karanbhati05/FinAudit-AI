package com.finaudit.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "System Status", description = "Public health, metadata, and API documentation discovery")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "API root discovery", description = "Returns active service metadata, version, and documentation links")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "name", "FinAudit AI — Autonomous Financial Audit Engine",
                "status", "UP",
                "version", "1.0.0",
                "swagger", "/swagger-ui/index.html",
                "apiDocs", "/v3/api-docs",
                "health", "/actuator/health",
                "demoCredentials", Map.of(
                        "email", "demo@finaudit.ai",
                        "password", "DemoAuditor2026!"
                )
        ));
    }

    @GetMapping("/health")
    @Operation(summary = "Lightweight health check", description = "Returns HTTP 200 UP for uptime monitors and load balancers")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
