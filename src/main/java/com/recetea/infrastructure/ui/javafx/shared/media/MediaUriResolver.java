package com.recetea.infrastructure.ui.javafx.shared.media;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;

/**
 * Builds a JavaFX-loadable URI for a media file from a base path + storage key,
 * supporting both deployment shapes:
 *
 * <ul>
 *   <li>cloud — base path is an HTTPS URL (e.g. Supabase public-read bucket): the
 *       storage key is appended to it directly;</li>
 *   <li>local — base path is a filesystem path: resolved to a {@code file:} URI
 *       via {@link File#toURI()}.</li>
 * </ul>
 *
 * Also exposes {@link #placeholder()} — a lazily-cached fallback image rendered
 * when the primary image fails to load (404 in the cloud, missing file locally).
 */
public final class MediaUriResolver {

    private static final String PLACEHOLDER_RESOURCE =
            "/com/recetea/infrastructure/ui/javafx/images/placeholder.png";

    private static volatile Image cachedPlaceholder;

    private MediaUriResolver() {}

    /**
     * @return a URI string consumable by {@link Image#Image(String)} / {@link Image#Image(String, boolean)},
     *         or {@code null} if either argument is null/blank.
     */
    public static String resolve(String basePath, String storageKey) {
        if (basePath == null || basePath.isBlank() || storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if (isCloud(basePath)) {
            return basePath.endsWith("/") ? basePath + storageKey : basePath + "/" + storageKey;
        }
        return new File(basePath, storageKey).toURI().toString();
    }

    /** True when the configured base path is an HTTPS/HTTP URL (cloud storage). */
    public static boolean isCloud(String basePath) {
        return basePath != null && (basePath.startsWith("http://") || basePath.startsWith("https://"));
    }

    /**
     * Lazy-loaded fallback image displayed when a primary image errors out.
     * Returns {@code null} if the placeholder asset itself is missing — callers
     * should null-check and degrade gracefully (e.g. show a CSS-styled empty state).
     */
    public static Image placeholder() {
        Image p = cachedPlaceholder;
        if (p == null) {
            synchronized (MediaUriResolver.class) {
                if (cachedPlaceholder == null) {
                    URL url = MediaUriResolver.class.getResource(PLACEHOLDER_RESOURCE);
                    cachedPlaceholder = (url != null) ? new Image(url.toExternalForm()) : null;
                }
                p = cachedPlaceholder;
            }
        }
        return p;
    }
}
