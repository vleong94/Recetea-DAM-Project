package com.recetea.infrastructure.persistence.recipe.jdbc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

/**
 * Immutable application configuration. Built once via {@link #load()} from a
 * three-tier precedence chain:
 *
 * <ol>
 *   <li><b>Environment variable</b> (e.g. {@code DB_URL}) — highest priority,
 *       suited for container/CI deployments where secrets must not live in files.</li>
 *   <li><b>System property</b> (e.g. {@code -Ddb.url=...}) — useful for local
 *       overrides during development without editing the properties file.</li>
 *   <li><b>{@code application-<env>.properties}</b> — packaged defaults, last resort.
 *       The {@code <env>} suffix is resolved from the {@code env} system property
 *       (falls back to {@code "local"}).</li>
 * </ol>
 *
 * Required fields ({@code dbUrl, dbUser, dbPassword}) throw if absent across all
 * three tiers; optional fields ({@code storageBasePath}, {@code supabaseAuthToken})
 * tolerate absence so unit tests that don't touch storage don't need them configured.
 *
 * <p><b>Storage path semantics</b>: {@link #storageBasePath()} is a {@link Path}
 * only when the configured value is a local filesystem path; HTTPS-shaped values
 * (Supabase / S3 / etc.) are detected during load and the Path field is null.
 * The literal string is preserved on {@link #storageBaseRaw()} regardless of shape,
 * so the storage-adapter factory can route on prefix without re-resolving.
 */
public record AppConfig(
        String dbUrl,
        String dbUser,
        String dbPassword,
        String dbSslMode,
        Path storageBasePath,
        String storageBaseRaw,
        String mediaCdnUrl,
        String supabaseAuthToken,
        int maxPoolSize,
        int connectionTimeout
) {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Default env-lookup wired to the live OS process environment. {@code System::getenv} is
     * a method reference so it cannot be reassigned at runtime; tests inject an alternative
     * via the package-private {@link #load(Function)} overload.
     */
    static final Function<String, String> SYSTEM_ENV = System::getenv;

    /** Builds a fresh AppConfig from the current environment. Not cached — callers may cache. */
    public static AppConfig load() {
        return load(SYSTEM_ENV);
    }

    /**
     * Integration-test entry point. Neutralises the environment-variable tier
     * of the precedence chain so the resolved configuration always comes from
     * the {@code application-<env>.properties} file (or a system-property
     * override). Use from {@code BaseRepositoryTest} and friends to pin tests
     * to {@code application-test.properties} regardless of any
     * {@code DB_URL} / {@code DB_USER} / {@code DB_PASSWORD} exported in the
     * developer's shell or IDE run config.
     *
     * <p>This exists because the production-shape {@link #load()} respects
     * env-var precedence (correct for normal app launches, dangerous for
     * tests that {@code TRUNCATE … RESTART IDENTITY CASCADE}). Without this
     * method, a stray {@code DB_URL=supabase://...} export silently routes
     * destructive integration tests at production data.
     */
    public static AppConfig loadForTesting() {
        return load(key -> null);
    }

    /**
     * Test-friendly overload: callers (production code) use {@link #load()} which delegates here
     * with {@link #SYSTEM_ENV}. Tests inject {@code key -> null} to neutralize the env-var tier
     * regardless of the host machine's exported variables, or a fixture-backed lookup to exercise
     * the env-tier precedence path deterministically. Package-private on purpose — production
     * code must not reach for this overload.
     */
    static AppConfig load(Function<String, String> envLookup) {
        Properties props = loadProperties();
        String storageRaw = resolve(envLookup, "STORAGE_BASE_PATH", "storage.base-path", props);
        return new AppConfig(
                require(envLookup, "DB_URL",                "db.url",               props),
                require(envLookup, "DB_USER",               "db.user",              props),
                require(envLookup, "DB_PASSWORD",           "db.password",          props),
                // sslMode: optional. Cloud providers (Neon, Supabase, AWS RDS) typically need
                // "require" or "verify-full". When null, HikariCP uses the JDBC default (prefer).
                resolve(envLookup, "DB_SSL_MODE", "db.sslMode", props),
                resolveStoragePath(storageRaw),
                storageRaw,
                // mediaCdnUrl: optional. When set, future cloud-storage adapters can derive
                // public URLs from it (e.g. https://cdn.example.com/{storageKey}).
                resolve(envLookup, "MEDIA_CDN_URL", "media.cdnUrl", props),
                // supabaseAuthToken: optional unless storageBaseRaw is an HTTPS URL pointing at
                // Supabase. The cloud storage adapter validates presence at construction time.
                resolve(envLookup, "SUPABASE_SERVICE_KEY", "supabase.serviceKey", props),
                intOrDefault(envLookup, "DB_MAX_POOL_SIZE",     "db.maxPoolSize",       props, 10),
                // 20s default — wide enough to absorb cloud cold-starts (Neon's auto-suspend
                // wakeup typically lands in 1–8s, Supabase free-tier in 2–12s). For a desktop
                // client a single 20s blocking connect on first launch is preferable to a
                // confusing pool-timeout error. Tighten via DB_CONNECTION_TIMEOUT in
                // environments with stricter SLAs or always-on databases.
                intOrDefault(envLookup, "DB_CONNECTION_TIMEOUT","db.connectionTimeout", props, 20_000)
        );
    }

    // ── Internal precedence chain ──────────────────────────────────────────

    /**
     * Strict three-tier precedence: {@code envLookup.apply(envKey)} > {@code System.getProperty(propKey)}
     * > {@code internalProperties.getProperty(propKey)}. Returns {@code null} if none of the three
     * tiers is set. Emits an INFO log line per resolution disclosing only the source tier (never
     * the value) so a portable {@code Recetea.exe} startup makes its config provenance auditable.
     *
     * <p>The {@code envLookup} parameter exists so {@link AppConfig#load(Function)} can hand in
     * either the live OS environment ({@code System::getenv}) or a deterministic test fixture.
     */
    static String resolve(Function<String, String> envLookup, String envKey, String propKey, Properties props) {
        String fromEnv = envLookup.apply(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            log.info("Configuration '{}' loaded from Environment Variable", envKey);
            return fromEnv;
        }
        String fromSys = System.getProperty(propKey);
        if (fromSys != null && !fromSys.isBlank()) {
            log.info("Configuration '{}' loaded from System Property '{}'", envKey, propKey);
            return fromSys;
        }
        String fromFile = props.getProperty(propKey);
        if (fromFile != null && !fromFile.isBlank()) {
            log.info("Configuration '{}' loaded from Internal Properties", envKey);
            return fromFile;
        }
        log.debug("Configuration '{}' is unset across all tiers (env / sysprop / properties)", envKey);
        return null;
    }

    private static String require(Function<String, String> envLookup, String envKey, String propKey, Properties props) {
        String value = resolve(envLookup, envKey, propKey, props);
        if (value == null) {
            throw new IllegalStateException(
                    "Missing required configuration: env " + envKey
                            + " / sysprop " + propKey + " / properties file");
        }
        return value;
    }

    private static int intOrDefault(Function<String, String> envLookup, String envKey, String propKey, Properties props, int defaultValue) {
        String value = resolve(envLookup, envKey, propKey, props);
        return (value != null) ? Integer.parseInt(value) : defaultValue;
    }

    /**
     * Returns a local Path only when {@code value} is a filesystem path. HTTPS / S3 / etc.
     * yield {@code null} here — the literal string lives on {@link #storageBaseRaw()} for
     * the storage adapter factory to route on. The {@code InvalidPathException} catch is
     * defence-in-depth: any future URL-like prefix that slips past the {@code http(s)} guard
     * (custom schemes, malformed inputs) must not crash a portable production startup.
     */
    private static Path resolveStoragePath(String value) {
        if (value == null) return null;
        if (value.startsWith("http://") || value.startsWith("https://")) return null;
        try {
            Path path = Path.of(value);
            Files.createDirectories(path);
            return path;
        } catch (InvalidPathException e) {
            log.warn("storage.base-path '{}' is not a local filesystem path; treating as remote URI", value);
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + value, e);
        }
    }

    private static Properties loadProperties() {
        String env = System.getProperty("env", "local");
        String fileName = "application-" + env + ".properties";
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalStateException("Configuration file not found on classpath: " + fileName);
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + fileName, e);
        }
        return props;
    }
}
