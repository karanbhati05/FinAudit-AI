package com.finaudit.api.controller;

import com.finaudit.api.service.ObservabilityService;
import com.finaudit.api.service.ScheduledCleanupService;
import com.finaudit.core.model.CleanupSummary;
import com.finaudit.core.model.ObservabilityMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Observability", description = "Operational health, Gemini quotas, rate limits, and audit latency metrics")
@SecurityRequirement(name = "bearerAuth")
public class AdminObservabilityController {

    private final ObservabilityService observabilityService;
    private final ScheduledCleanupService scheduledCleanupService;

    public AdminObservabilityController(
            ObservabilityService observabilityService,
            ScheduledCleanupService scheduledCleanupService
    ) {
        this.observabilityService = observabilityService;
        this.scheduledCleanupService = scheduledCleanupService;
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system observability metrics", description = "Returns reports processed today, circuit breaker status, quota counters, average audit latency, and 24h failure count")
    @ApiResponse(responseCode = "200", description = "Operational metrics retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required")
    public ResponseEntity<ObservabilityMetricsResponse> getMetrics() {
        return ResponseEntity.ok(observabilityService.getObservabilityMetrics());
    }

    @PostMapping("/maintenance/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger storage hygiene and demo cleanup", description = "Executes the 30-day retention cleanup and resets the demo account to canonical reports")
    @ApiResponse(responseCode = "200", description = "Maintenance cleanup completed successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required")
    public ResponseEntity<CleanupSummary> triggerCleanup() {
        return ResponseEntity.ok(scheduledCleanupService.runScheduledCleanup());
    }
}
