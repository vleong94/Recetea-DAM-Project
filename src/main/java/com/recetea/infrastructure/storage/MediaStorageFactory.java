package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Selects the concrete {@link IMediaStorageService} implementation based on the
 * shape of {@link AppConfig#storageBaseRaw()}:
 *
 * <ul>
 *   <li>Value starts with {@code http://} or {@code https://} → {@link SupabaseMediaStorage}.
 *       Requires {@code SUPABASE_SERVICE_KEY} (env / sysprop / file) — adapter throws otherwise.</li>
 *   <li>Anything else (including null/blank) → {@link LocalFileSystemMediaStorage} pointing at
 *       either the configured path or a temp-dir fallback ({@link #resolveBasePath}).</li>
 * </ul>
 *
 * <p>{@link #resolveBasePath} is the single source of truth for the "effective" storage root.
 * The composition root calls it once at startup, hands the result to {@link #create} (which
 * builds the storage adapter) and to the {@code NavigationService} (which threads the same
 * value down to the UI's {@code MediaUriResolver}). Same string everywhere — no drift.
 */
public final class MediaStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageFactory.class);

    private MediaStorageFactory() {}

    /**
     * Returns the effective storage root: the configured value when present, otherwise an
     * ephemeral {@code <java.io.tmpdir>/recetea-media} directory created on demand. Never null.
     *
     * <p>HTTPS-shaped configurations are returned verbatim; the local-filesystem fallback only
     * fires when the configured value is absent or blank.
     */
    public static String resolveBasePath(AppConfig config) {
        String raw = config.storageBaseRaw();
        if (raw != null && !raw.isBlank()) return raw;

        Path fallback = Path.of(System.getProperty("java.io.tmpdir"), "recetea-media");
        try {
            Files.createDirectories(fallback);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create fallback storage directory: " + fallback, e);
        }
        log.warn("storage.base-path not configured — falling back to ephemeral temp dir: {}. " +
                "Set STORAGE_BASE_PATH for persistent storage, or wire a cloud adapter via SPI.", fallback);
        return fallback.toString();
    }

    public static IMediaStorageService create(AppConfig config) {
        String resolved = resolveBasePath(config);
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            return new SupabaseMediaStorage(resolved, config.supabaseAuthToken());
        }
        return new LocalFileSystemMediaStorage(Path.of(resolved));
    }
}
