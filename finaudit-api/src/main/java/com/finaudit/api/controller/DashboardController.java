package com.finaudit.api.controller;

import com.finaudit.api.service.DashboardService;
import com.finaudit.core.model.DashboardSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "High-level metrics, aggregated compliance stats, and risk distribution")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated audit dashboard summary", description = "Returns portfolio metrics, spend/risk trend line, score distribution histogram, and policy leaderboard filtered by date range (7d, 30d, 90d, all)")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(name = "range", required = false, defaultValue = "30d") String range
    ) {
        return ResponseEntity.ok(dashboardService.getSummary(range));
    }
}
