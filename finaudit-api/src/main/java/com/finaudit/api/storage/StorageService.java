package com.finaudit.api.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {
    Path store(Long reportId, String filename, InputStream inputStream);
    Path getPath(Long reportId, String filename);
    InputStream load(Long reportId, String filename);
    void delete(Long reportId);
    long getTotalStorageBytes();
}
