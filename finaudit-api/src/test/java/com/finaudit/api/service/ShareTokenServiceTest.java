package com.finaudit.api.service;

import com.finaudit.api.exception.ShareLinkExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareTokenServiceTest {

    private final String testSecret = "secret-key-for-testing-share-token-generation-and-validation-must-be-long-enough";
    private final ShareTokenService shareTokenService = new ShareTokenService(testSecret, 7);

    @Test
    @DisplayName("Should successfully generate a token and extract the original reportId")
    void shouldGenerateAndExtractValidReportId() {
        Long originalReportId = 12345L;
        String token = shareTokenService.generateShareToken(originalReportId);

        assertThat(token).isNotBlank();

        Long extractedId = shareTokenService.validateAndExtractReportId(token);
        assertThat(extractedId).isEqualTo(originalReportId);
    }

    @Test
    @DisplayName("Should reject and throw ShareLinkExpiredException when token has passed its expiry")
    void shouldRejectExpiredShareToken() throws InterruptedException {
        Long originalReportId = 9999L;
        // Generate a token with 1ms validity
        String shortLivedToken = shareTokenService.generateShareToken(originalReportId, 1L);

        // Wait 15ms so token is definitively expired
        Thread.sleep(15);

        assertThatThrownBy(() -> shareTokenService.validateAndExtractReportId(shortLivedToken))
                .isInstanceOf(ShareLinkExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject tampered or corrupted token signature with IllegalArgumentException")
    void shouldRejectTamperedToken() {
        String token = shareTokenService.generateShareToken(42L);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> shareTokenService.validateAndExtractReportId(tamperedToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject blank or null share token")
    void shouldRejectBlankToken() {
        assertThatThrownBy(() -> shareTokenService.validateAndExtractReportId("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> shareTokenService.validateAndExtractReportId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
