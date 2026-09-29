package com.finaudit.api.service;

import com.finaudit.api.exception.CapacityExceededException;
import com.finaudit.api.exception.CircuitBreakerOpenException;
import com.finaudit.api.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enforces production financial cost guardrails, rate limits, concurrency caps,
 * and circuit-breaker thresholds on LLM and ingestion operations.
 */
@Service
public class CostGuardrailService {

    private static final Logger log = LoggerFactory.getLogger(CostGuardrailService.class);

    public static final int DEFAULT_MAX_UPLOADS_PER_USER_DAILY = 5;
    public static final int DEFAULT_MAX_UPLOADS_PER_IP_DAILY = 10;
    public static final int DEFAULT_MAX_CONCURRENT_AUDITS = 2;
    public static final int DEFAULT_MAX_DAILY_GEMINI_CALLS = 100;
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    private final int maxUploadsPerUser;
    private final int maxUploadsPerIp;
    private final int maxDailyGeminiCalls;
    private final Semaphore auditSemaphore;

    private final Map<String, AtomicInteger> userUploadCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> ipUploadCounts = new ConcurrentHashMap<>();
    private final AtomicInteger dailyGeminiCallCount = new AtomicInteger(0);

    public CostGuardrailService(
            @Value("${finaudit.guardrails.max-uploads-per-user:5}") int maxUploadsPerUser,
            @Value("${finaudit.guardrails.max-uploads-per-ip:10}") int maxUploadsPerIp,
            @Value("${finaudit.guardrails.max-concurrent-audits:2}") int maxConcurrentAudits,
            @Value("${finaudit.guardrails.max-daily-gemini-calls:100}") int maxDailyGeminiCalls
    ) {
        this.maxUploadsPerUser = maxUploadsPerUser;
        this.maxUploadsPerIp = maxUploadsPerIp;
        this.maxDailyGeminiCalls = maxDailyGeminiCalls;
        this.auditSemaphore = new Semaphore(maxConcurrentAudits);
        log.info("Initialized CostGuardrailService with userLimit={}, ipLimit={}, concurrencyLimit={}, dailyGeminiQuota={}",
                maxUploadsPerUser, maxUploadsPerIp, maxConcurrentAudits, maxDailyGeminiCalls);
    }

    /**
     * Checks rate limits per user account and client IP address.
     */
    public void enforceUploadRateLimits(String userIdentifier, String clientIp) {
        long secondsUntilMidnight = getSecondsUntilMidnightUtc();

        // 1. Check circuit breaker first
        if (dailyGeminiCallCount.get() >= maxDailyGeminiCalls) {
            log.warn("Circuit breaker OPEN: Daily Gemini quota reached ({}/{})", dailyGeminiCallCount.get(), maxDailyGeminiCalls);
            throw new CircuitBreakerOpenException(
                    "The public demo has reached its daily AI audit quota (" + maxDailyGeminiCalls + " audits/day). " +
                            "To preserve API costs, further live uploads are paused until midnight UTC. " +
                            "Please explore the pre-audited sample reports in the dashboard.",
                    secondsUntilMidnight
            );
        }

        // 2. User upload rate limit
        if (userIdentifier != null && !userIdentifier.isBlank()) {
            AtomicInteger userCounter = userUploadCounts.computeIfAbsent(userIdentifier, k -> new AtomicInteger(0));
            if (userCounter.get() >= maxUploadsPerUser) {
                log.warn("Rate limit exceeded for user: {} (count: {})", userIdentifier, userCounter.get());
                throw new RateLimitExceededException(
                        "Daily upload limit reached for your account (maximum " + maxUploadsPerUser + " uploads per day). " +
                                "Please explore existing audited reports or return tomorrow.",
                        secondsUntilMidnight
                );
            }
        }

        // 3. IP address upload rate limit
        if (clientIp != null && !clientIp.isBlank()) {
            AtomicInteger ipCounter = ipUploadCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
            if (ipCounter.get() >= maxUploadsPerIp) {
                log.warn("Rate limit exceeded for IP: {} (count: {})", clientIp, ipCounter.get());
                throw new RateLimitExceededException(
                        "Daily upload limit reached for your IP address (maximum " + maxUploadsPerIp + " uploads per day). " +
                                "Please try again tomorrow.",
                        secondsUntilMidnight
                );
            }
        }
    }

    /**
     * Increments the upload counter for user and IP.
     */
    public void recordUpload(String userIdentifier, String clientIp) {
        if (userIdentifier != null && !userIdentifier.isBlank()) {
            userUploadCounts.computeIfAbsent(userIdentifier, k -> new AtomicInteger(0)).incrementAndGet();
        }
        if (clientIp != null && !clientIp.isBlank()) {
            ipUploadCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    /**
     * Acquires a concurrency permit for running an audit with Gemini.
     */
    public boolean tryAcquireAuditSlot() {
        try {
            boolean acquired = auditSemaphore.tryAcquire(2, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Audit concurrency limit exceeded. Available permits: {}", auditSemaphore.availablePermits());
                throw new CapacityExceededException(
                        "Audit pipeline is currently operating at maximum concurrency. Please retry shortly.",
                        15
                );
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CapacityExceededException("Interrupted while awaiting audit capacity.", 5);
        }
    }

    /**
     * Releases the audit concurrency permit.
     */
    public void releaseAuditSlot() {
        auditSemaphore.release();
    }

    /**
     * Records a successful or in-flight Gemini LLM call towards the daily spend limit.
     */
    public int recordGeminiCall() {
        int currentCount = dailyGeminiCallCount.incrementAndGet();
        log.info("Recorded Gemini AI call. Daily usage: {}/{}", currentCount, maxDailyGeminiCalls);
        return currentCount;
    }

    /**
     * Validates file size, extension, and content structure (PDF magic bytes / UTF-8 plain text).
     */
    public void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size (" + (file.getSize() / 1024 / 1024) + "MB) exceeds the maximum allowed 10MB limit.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name must be specified.");
        }

        String lower = filename.toLowerCase();
        boolean hasValidExt = lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".csv");
        if (!hasValidExt) {
            throw new IllegalArgumentException("Unsupported file type. Only PDF documents (.pdf) and plain text reports (.txt, .csv) are supported.");
        }

        // Validate content structure / magic bytes
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read > 0 && lower.endsWith(".pdf")) {
                String magic = new String(header, 0, Math.min(read, 5), StandardCharsets.US_ASCII);
                if (!magic.startsWith("%PDF-")) {
                    throw new IllegalArgumentException("Invalid file: The file extension is .pdf, but the file content does not match a valid PDF header format.");
                }
            } else if (read > 0) {
                // Check for non-printable null bytes commonly in executable/zip files
                for (int i = 0; i < read; i++) {
                    if (header[i] == 0) {
                        throw new IllegalArgumentException("Invalid file: Binary data detected. Only plain text or PDF files are accepted.");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read and inspect uploaded file header: " + e.getMessage());
        }
    }

    /**
     * Scheduled reset at midnight UTC every day.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void resetDailyLimits() {
        log.info("Resetting daily upload counts and Gemini call quota at midnight UTC.");
        userUploadCounts.clear();
        ipUploadCounts.clear();
        dailyGeminiCallCount.set(0);
    }

    public int getDailyGeminiCallCount() {
        return dailyGeminiCallCount.get();
    }

    public int getMaxDailyGeminiCalls() {
        return maxDailyGeminiCalls;
    }

    public int getMaxUploadsPerUser() {
        return maxUploadsPerUser;
    }

    public int getMaxUploadsPerIp() {
        return maxUploadsPerIp;
    }

    private long getSecondsUntilMidnightUtc() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC);
        return Duration.between(now, midnight).getSeconds();
    }
}
