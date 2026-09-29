package com.finaudit.api.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    private LocalStorageService storageService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("store, getPath, load, and delete should manage report files on disk")
    void shouldStoreAndLoadFile() throws IOException {
        String content = "Hello Financial World";
        ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        Path storedPath = storageService.store(100L, "report.pdf", is);
        assertThat(storedPath).exists();

        Path path = storageService.getPath(100L, "report.pdf");
        assertThat(path).isEqualTo(storedPath);

        try (InputStream in = storageService.load(100L, "report.pdf")) {
            String read = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(read).isEqualTo("Hello Financial World");
        }

        storageService.delete(100L);
        assertThat(storedPath).doesNotExist();
    }

    @Test
    @DisplayName("load should throw IllegalArgumentException for nonexistent file")
    void shouldThrowWhenFileNotFound() {
        assertThatThrownBy(() -> storageService.load(999L, "nonexistent.pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
