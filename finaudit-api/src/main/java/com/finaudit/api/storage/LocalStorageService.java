package com.finaudit.api.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${finaudit.storage.location:storage/reports}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public Path store(Long reportId, String filename, InputStream inputStream) {
        try {
            Path reportDir = rootLocation.resolve(String.valueOf(reportId)).normalize();
            Files.createDirectories(reportDir);

            // Clean filename
            String cleanFilename = Paths.get(filename).getFileName().toString();
            Path destinationFile = reportDir.resolve(cleanFilename).normalize();

            // Guard against path traversal
            if (!destinationFile.getParent().equals(reportDir)) {
                throw new SecurityException("Cannot store file outside current directory.");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return destinationFile;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file for report: " + reportId, e);
        }
    }

    @Override
    public Path getPath(Long reportId, String filename) {
        return rootLocation.resolve(String.valueOf(reportId)).resolve(filename).normalize();
    }

    @Override
    public InputStream load(Long reportId, String filename) {
        try {
            Path filePath = getPath(reportId, filename);
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("File not found: " + filename + " for report " + reportId);
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file for report: " + reportId, e);
        }
    }

    @Override
    public void delete(Long reportId) {
        Path reportDir = rootLocation.resolve(String.valueOf(reportId)).normalize();
        if (Files.exists(reportDir)) {
            try {
                Files.walk(reportDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete directory for report: " + reportId, e);
            }
        }
    }
}
