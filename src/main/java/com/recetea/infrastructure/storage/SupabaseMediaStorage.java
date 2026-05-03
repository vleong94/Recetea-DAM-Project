package com.recetea.infrastructure.storage;

import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Cloud media storage adapter backed by Supabase Storage REST API.
 *
 * <p><b>URL derivation</b>. Supabase exposes two URL shapes for the same bucket:
 * <ul>
 *   <li>public-read URL — {@code https://<project>.supabase.co/storage/v1/object/public/<bucket>/}</li>
 *   <li>upload URL —      {@code https://<project>.supabase.co/storage/v1/object/<bucket>/}</li>
 * </ul>
 * Configuration carries the public URL (because that's what gets persisted into
 * the {@code recipe_media.storage_provider} field for client-side display); the
 * upload URL is derived by stripping the {@code /public/} segment.
 *
 * <p><b>Auth</b>. Uploads and deletes require a Bearer token (the Supabase
 * service-role key in production, or anon key for permissive RLS policies).
 * Sourced from {@code SUPABASE_SERVICE_KEY} env / {@code supabase.serviceKey}
 * sysprop / properties file via {@code AppConfig.supabaseAuthToken()}. A blank
 * token throws at construction so misconfigured deployments fail at startup
 * rather than on the first upload.
 */
public class SupabaseMediaStorage implements IMediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseMediaStorage.class);

    private static final String PUBLIC_SEGMENT = "/object/public/";
    private static final String UPLOAD_SEGMENT = "/object/";

    private final OkHttpClient httpClient;
    private final String       uploadBaseUrl;
    private final String       authToken;

    /**
     * @param publicReadUrl Supabase public-read URL for the bucket, with or without trailing slash.
     *                      Must contain {@code /object/public/<bucket>/}.
     * @param authToken     Bearer token (typically the service role key); blank tokens are rejected.
     */
    public SupabaseMediaStorage(String publicReadUrl, String authToken) {
        this(publicReadUrl, authToken, defaultClient());
    }

    /**
     * Test-friendly constructor: lets the integration suite inject an {@link OkHttpClient}
     * with aggressive timeouts (so a queued slow response triggers a {@code SocketTimeoutException}
     * within the test's wall-clock budget) without dragging those timeouts into production.
     * Package-private on purpose — production code must always go through the public ctor.
     */
    SupabaseMediaStorage(String publicReadUrl, String authToken, OkHttpClient httpClient) {
        if (publicReadUrl == null || publicReadUrl.isBlank()) {
            throw new IllegalArgumentException("Supabase public read URL is required");
        }
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalStateException(
                    "SUPABASE_SERVICE_KEY is required when STORAGE_BASE_PATH points at Supabase. "
                            + "Set the env var (or -Dsupabase.serviceKey=...) before launching with -Denv=prod.");
        }
        String normalized = publicReadUrl.endsWith("/") ? publicReadUrl : publicReadUrl + "/";
        if (!normalized.contains(PUBLIC_SEGMENT)) {
            throw new IllegalArgumentException(
                    "Supabase public read URL must contain '" + PUBLIC_SEGMENT + "<bucket>/' — got: " + publicReadUrl);
        }
        this.uploadBaseUrl = normalized.replace(PUBLIC_SEGMENT, UPLOAD_SEGMENT);
        this.authToken     = authToken;
        this.httpClient    = httpClient;
        log.info("SupabaseMediaStorage initialised — upload endpoint resolved to {}", uploadBaseUrl);
    }

    private static OkHttpClient defaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .writeTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public StorageResult store(InputStream data, String originalName) {
        String filename = UUID.randomUUID() + buildExtensionSuffix(originalName);
        String mimeType = guessMimeType(originalName);
        byte[] payload;
        try {
            payload = data.readAllBytes();
        } catch (IOException e) {
            throw new InfrastructureException("Failed to read upload stream for: " + filename, e);
        }
        Request request = new Request.Builder()
                .url(uploadBaseUrl + filename)
                .header("Authorization", "Bearer " + authToken)
                .post(RequestBody.create(payload, MediaType.parse(mimeType)))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Include the HTTP reason phrase ("Forbidden", "Payload Too Large", etc.) so the
                // wrapped InfrastructureException's message tells operators *what* failed without
                // needing to look up the status-code → meaning mapping. The body is appended too
                // when present (Supabase's JSON error shape carries useful diagnostic detail).
                throw new InfrastructureException(
                        "Supabase upload failed (" + response.code() + " " + response.message()
                                + ") for: " + filename + " — " + readBodySafe(response), null);
            }
            return new StorageResult(filename, payload.length, mimeType);
        } catch (IOException e) {
            throw new InfrastructureException("Supabase upload IO error for: " + filename, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Request request = new Request.Builder()
                .url(uploadBaseUrl + storageKey)
                .header("Authorization", "Bearer " + authToken)
                .delete()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            // 404 is acceptable — already deleted is the desired end state.
            if (!response.isSuccessful() && response.code() != 404) {
                throw new InfrastructureException(
                        "Supabase delete failed (" + response.code() + " " + response.message()
                                + ") for: " + storageKey + " — " + readBodySafe(response), null);
            }
        } catch (IOException e) {
            throw new InfrastructureException("Supabase delete IO error for: " + storageKey, e);
        }
    }

    private static String buildExtensionSuffix(String originalName) {
        if (originalName == null) return "";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 && dot < originalName.length() - 1
                ? "." + originalName.substring(dot + 1)
                : "";
    }

    private static String guessMimeType(String originalName) {
        if (originalName == null) return "application/octet-stream";
        String lower = originalName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    private static String readBodySafe(Response response) {
        try (ResponseBody body = response.body()) {
            return body != null ? body.string() : "<no body>";
        } catch (IOException e) {
            return "<failed to read body: " + e.getMessage() + ">";
        }
    }
}
