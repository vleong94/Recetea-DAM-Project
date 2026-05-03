package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local-filesystem implementation of {@link IMediaStorageService}. Used in
 * development and as the temp-dir fallback when no storage path is
 * configured.
 *
 * <p>Files are stored flat under {@code basePath} (no subdirectory tree)
 * with a {@link UUID}-prefixed filename and the original extension
 * preserved. The flat layout keeps the storage key opaque from the
 * adapter's perspective and avoids tree-traversal concerns when
 * resolving URIs at the UI layer.
 *
 * <p>MIME type is sniffed via {@link Files#probeContentType} (content,
 * not extension). When detection fails the type collapses to
 * {@code application/octet-stream} — generic but never wrong.
 *
 * <p><b>ES — </b>Implementación de {@link IMediaStorageService} sobre
 * sistema de archivos local. Se usa en desarrollo y como fallback de
 * directorio temporal cuando no hay ruta de storage configurada.
 *
 * <p>Los archivos se guardan planos bajo {@code basePath} (sin árbol
 * de subdirectorios) con un nombre prefijado por {@link UUID} y la
 * extensión original preservada. El layout plano mantiene la
 * storage key opaca desde el punto de vista del adaptador y evita
 * preocupaciones de recorrido de árbol al resolver URIs en la capa
 * de UI.
 *
 * <p>El tipo MIME se detecta vía {@link Files#probeContentType}
 * (contenido, no extensión). Cuando la detección falla, el tipo
 * colapsa a {@code application/octet-stream} — genérico pero nunca
 * incorrecto.
 */
public class LocalFileSystemMediaStorage implements IMediaStorageService {

    private final Path basePath;

    /**
     * Path is injected explicitly by {@link MediaStorageFactory#create} — no SPI lookup
     * here, no static config singleton. The factory resolves the effective base path
     * (configured value or temp-dir fallback) once at the composition root and threads
     * it down through this constructor.
     */
    public LocalFileSystemMediaStorage(Path basePath) {
        this.basePath = basePath;
    }

    @Override
    public StorageResult store(InputStream data, String originalName) {
        String filename = UUID.randomUUID() + buildExtensionSuffix(originalName);
        Path target = basePath.resolve(filename);
        try {
            long sizeBytes = Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            String mimeType = Files.probeContentType(target);
            if (mimeType == null) mimeType = "application/octet-stream";
            return new StorageResult(filename, sizeBytes, mimeType);
        } catch (IOException e) {
            throw new InfrastructureException("Failed to store media file: " + filename, e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(basePath.resolve(path));
        } catch (IOException e) {
            throw new InfrastructureException("Failed to delete media file: " + path, e);
        }
    }

    private String buildExtensionSuffix(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 && dot < originalName.length() - 1 ? "." + originalName.substring(dot + 1) : "";
    }
}
