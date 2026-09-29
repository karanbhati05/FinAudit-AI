package com.finaudit.api.service;

import com.finaudit.api.exception.CapacityExceededException;
import com.finaudit.api.exception.CircuitBreakerOpenException;
import com.finaudit.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostGuardrailServiceTest {

    private CostGuardrailService guardrailService;

    @BeforeEach
    void setUp() {
        // userLimit = 5, ipLimit = 10, concurrency = 2, dailyGeminiQuota = 100
        guardrailService = new CostGuardrailService(5, 10, 2, 100);
    }

    @Test
    @DisplayName("Should allow up to 5 uploads per user and block the 6th with RateLimitExceededException")
    void shouldEnforceUserUploadLimit() {
        String userId = "auditor@test.com";
        String ip = "192.168.1.1";

        for (int i = 0; i < 5; i++) {
            guardrailService.enforceUploadRateLimits(userId, ip);
            guardrailService.recordUpload(userId, ip);
        }

        assertThatThrownBy(() -> guardrailService.enforceUploadRateLimits(userId, ip))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Daily upload limit reached for your account (maximum 5 uploads per day)");
    }

    @Test
    @DisplayName("Should allow up to 10 uploads per IP and block the 11th with RateLimitExceededException")
    void shouldEnforceIpUploadLimit() {
        String ip = "10.0.0.1";

        for (int i = 0; i < 10; i++) {
            String user = "user-" + i;
            guardrailService.enforceUploadRateLimits(user, ip);
            guardrailService.recordUpload(user, ip);
        }

        assertThatThrownBy(() -> guardrailService.enforceUploadRateLimits("user-11", ip))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Daily upload limit reached for your IP address (maximum 10 uploads per day)");
    }

    @Test
    @DisplayName("Should block audits when concurrency limit of 2 is exceeded")
    void shouldEnforceConcurrencyLimit() {
        guardrailService.tryAcquireAuditSlot();
        guardrailService.tryAcquireAuditSlot();

        // 3rd concurrent attempt should fail
        assertThatThrownBy(() -> guardrailService.tryAcquireAuditSlot())
                .isInstanceOf(CapacityExceededException.class)
                .hasMessageContaining("maximum concurrency");

        // Release one slot
        guardrailService.releaseAuditSlot();

        // Now should succeed
        assertThat(guardrailService.tryAcquireAuditSlot()).isTrue();
        guardrailService.releaseAuditSlot();
    }

    @Test
    @DisplayName("Should trigger circuit breaker when daily Gemini quota of 100 is exceeded")
    void shouldTripCircuitBreaker() {
        for (int i = 0; i < 100; i++) {
            guardrailService.recordGeminiCall();
        }

        assertThat(guardrailService.getDailyGeminiCallCount()).isEqualTo(100);

        assertThatThrownBy(() -> guardrailService.enforceUploadRateLimits("any@test.com", "1.1.1.1"))
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("daily AI audit quota (100 audits/day)");
    }

    @Test
    @DisplayName("Should validate valid PDF file with %PDF- magic bytes")
    void shouldValidateValidPdf() {
        MockMultipartFile validPdf = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "%PDF-1.4\n1 0 obj\n<<>>\nendobj".getBytes(StandardCharsets.UTF_8)
        );

        guardrailService.validateUploadFile(validPdf);
    }

    @Test
    @DisplayName("Should reject invalid PDF file lacking %PDF- magic bytes")
    void shouldRejectInvalidPdfMagicBytes() {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "THIS IS NOT A PDF".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> guardrailService.validateUploadFile(fakePdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match a valid PDF header format");
    }

    @Test
    @DisplayName("Should reject unsupported file extensions")
    void shouldRejectUnsupportedExtension() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "MZ...".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> guardrailService.validateUploadFile(exeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }
}
