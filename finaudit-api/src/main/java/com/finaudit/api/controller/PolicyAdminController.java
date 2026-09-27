package com.finaudit.api.controller;

import com.finaudit.api.service.PolicyIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/policies")
@Tag(name = "Admin Policy Management", description = "Endpoints for managing compliance policy vector store index")
public class PolicyAdminController {

    private final PolicyIngestionService policyIngestionService;

    public PolicyAdminController(PolicyIngestionService policyIngestionService) {
        this.policyIngestionService = policyIngestionService;
    }

    // TODO: Milestone 9 — Security & RBAC: Guard with ROLE_ADMIN via @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex compliance policies", description = "Re-parses the policy documents and regenerates vector embeddings in pgvector (Admin only)")
    public ResponseEntity<Map<String, Object>> reindexPolicies() {
        int count = policyIngestionService.ingestSeedPolicies();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Reindexed compliance policies into pgvector",
                "policyCount", count
        ));
    }
}
