package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LocalFileSystemMediaStorage implements IMediaStorageService {

    private final Path basePath;

    public LocalFileSystemMediaStorage(Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public StorageResult store(byte[] data, String originalName) {
        String filename = UUID.randomUUID() + buildExtensionSuffix(originalName);
        Path target = basePath.resolve(filename);
        try {
            Files.write(target, data);
            String mimeType = Files.probeContentType(target);
            if (mimeType == null) mimeType = "application/octet-stream";
            long sizeBytes = target.toFile().length();
            return new StorageResult(filename, sizeBytes, mimeType);
        } catch (IOException e) {
            throw new InfrastructureException("Error al guardar el archivo multimedia: " + filename, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(basePath.resolve(path));
        } catch (IOException e) {
            throw new InfrastructureException("Error al eliminar el archivo multimedia: " + path, e);
        }
    }

    private String buildExtensionSuffix(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 && dot < originalName.length() - 1 ? "." + originalName.substring(dot + 1) : "";
    }
}
