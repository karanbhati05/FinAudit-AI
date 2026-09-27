package com.finaudit.api.controller;

import com.finaudit.api.entity.Report;
import com.finaudit.api.repository.ReportLineItemRepository;
import com.finaudit.api.repository.ReportRepository;
import com.finaudit.api.service.ReportParsingService;
import com.finaudit.api.storage.StorageService;
import com.finaudit.core.model.ReportStatus;
import com.finaudit.core.model.ReportStatusResponse;
import com.finaudit.core.model.ReportUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import com.finaudit.api.entity.AuditFinding;
import com.finaudit.api.entity.AuditRun;
import com.finaudit.api.repository.AuditFindingRepository;
import com.finaudit.api.repository.AuditRunRepository;
import com.finaudit.api.repository.ReportSpecifications;
import com.finaudit.api.service.ReportQueryService;
import com.finaudit.core.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Financial Report ingestion, parsing status, and auditing")
public class ReportController {

    private final ReportRepository reportRepository;
    private final ReportLineItemRepository lineItemRepository;
    private final StorageService storageService;
    private final ReportParsingService reportParsingService;
    private final AuditRunRepository auditRunRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final ReportQueryService reportQueryService;

    public ReportController(
            ReportRepository reportRepository,
            ReportLineItemRepository lineItemRepository,
            StorageService storageService,
            ReportParsingService reportParsingService,
            AuditRunRepository auditRunRepository,
            AuditFindingRepository auditFindingRepository,
            ReportQueryService reportQueryService
    ) {
        this.reportRepository = reportRepository;
        this.lineItemRepository = lineItemRepository;
        this.storageService = storageService;
        this.reportParsingService = reportParsingService;
        this.auditRunRepository = auditRunRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.reportQueryService = reportQueryService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a financial report", description = "Uploads a PDF or text document for AI parsing and auditing")
    @ApiResponse(responseCode = "202", description = "Report uploaded and parsing initiated")
    public ResponseEntity<ReportUploadResponse> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ownerId", required = false) Long ownerId
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ReportUploadResponse(null, "Uploaded file must not be empty."));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "uploaded_report.pdf";
        }

        String lower = originalFilename.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".txt") && !lower.endsWith(".csv")) {
            return ResponseEntity.badRequest().body(new ReportUploadResponse(null, "Only PDF and plain text (.txt, .csv) files are supported."));
        }

        // 1. Initial entity save to generate report ID
        Report report = new Report(ownerId, originalFilename, "pending");
        report = reportRepository.save(report);

        // 2. Save physical file to storage/reports/{reportId}/
        Path storedPath = storageService.store(report.getId(), originalFilename, file.getInputStream());
        report.setStoragePath(storedPath.toString());
        report.setStatus(ReportStatus.UPLOADED);
        reportRepository.save(report);

        // 3. Trigger asynchronous parsing on virtual threads
        reportParsingService.parseReportAsync(report.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ReportUploadResponse(report.getId(), "Report uploaded successfully. Parsing in progress."));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get report ingestion status", description = "Returns the status and number of extracted line items for a report")
    public ResponseEntity<ReportStatusResponse> getReportStatus(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id)
                .orElse(null);

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        int count = lineItemRepository.findByReportId(id).size();
        return ResponseEntity.ok(new ReportStatusResponse(
                report.getId(),
                report.getStatus(),
                count,
                report.getErrorReason()
        ));
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Get audit run and findings", description = "Returns the compliance score, risk level, and all policy findings for the report")
    public ResponseEntity<AuditReportResponse> getReportAudit(@PathVariable("id") Long id) {
        Report report = reportRepository.findById(id).orElse(null);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        AuditRun run = auditRunRepository.findByReportId(id).orElse(null);
        if (run == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<AuditFinding> findings = auditFindingRepository.findByReportId(id);
        List<AuditFindingResponse> findingResponses = findings.stream()
                .map(f -> new AuditFindingResponse(
                        f.getId(),
                        f.getLineItemId(),
                        f.getRuleSource(),
                        f.getSeverity(),
                        f.getDescription(),
                        f.getPolicyReference(),
                        f.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(new AuditReportResponse(
                id,
                run.getComplianceScore(),
                run.getRiskLevel(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getRawModelOutputJson(),
                findingResponses
        ));
    }

    @GetMapping
    @Operation(summary = "List paginated reports", description = "Query reports with pagination, sorting, and filters (status, riskLevel, date range)")
    public ResponseEntity<Page<ReportSummaryDto>> listReports(
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "riskLevel", required = false) RiskLevel riskLevel,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(value = "ownerId", required = false) Long ownerId,
            @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        Specification<Report> spec = Specification.where(ReportSpecifications.withStatus(status))
                .and(ReportSpecifications.withRiskLevel(riskLevel))
                .and(ReportSpecifications.withUploadedBetween(startDate, endDate))
                .and(ReportSpecifications.withOwnerId(ownerId));

        return ResponseEntity.ok(reportQueryService.getReports(spec, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complete report details", description = "Returns full detail of report, line items, audit run, and findings with resolved policy text")
    public ResponseEntity<ReportDetailResponse> getReportDetail(@PathVariable("id") Long id) {
        return reportQueryService.getReportDetail(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
