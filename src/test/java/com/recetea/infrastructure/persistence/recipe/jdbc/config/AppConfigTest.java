package com.recetea.infrastructure.persistence.recipe.jdbc.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the env-var → sys-prop → properties precedence chain. The env-var tier is now
 * directly observable: {@link AppConfig#load(Function)} accepts an injectable env-lookup,
 * so the suite supplies {@link #NO_ENV} (always returns {@code null}) to neutralize whatever
 * the host shell has exported, and a fixture lookup to exercise the env-tier path.
 *
 * <p>Why the injection: developer shells routinely export {@code DB_URL} / {@code DB_USER} /
 * {@code DB_PASSWORD} / {@code SUPABASE_SERVICE_KEY} / {@code STORAGE_BASE_PATH} so the
 * desktop client can talk to Supabase. Without the injection these env vars would win the
 * precedence chain inside the test JVM, breaking the file-tier and sys-prop-tier assertions
 * deterministically on any developer machine and non-deterministically in CI.
 */
@DisplayName("AppConfig — environment override precedence")
class AppConfigTest {

    /** Empty env lookup: makes the suite invariant against the host machine's exported vars. */
    private static final Function<String, String> NO_ENV = key -> null;

    private String previousEnv;
    private String previousDbUrl;
    private String previousSslMode;
    private String previousCdnUrl;

    @BeforeEach
    void pinTestEnvironment() {
        previousEnv     = System.getProperty("env");
        previousDbUrl   = System.getProperty("db.url");
        previousSslMode = System.getProperty("db.sslMode");
        previousCdnUrl  = System.getProperty("media.cdnUrl");
        System.setProperty("env", "test");
    }

    @AfterEach
    void restoreEnvironment() {
        restoreOrClear("env",          previousEnv);
        restoreOrClear("db.url",       previousDbUrl);
        restoreOrClear("db.sslMode",   previousSslMode);
        restoreOrClear("media.cdnUrl", previousCdnUrl);
    }

    private static void restoreOrClear(String key, String previous) {
        if (previous == null) System.clearProperty(key);
        else                  System.setProperty(key, previous);
    }

    @Test
    @DisplayName("Without overrides, AppConfig reflects application-test.properties values")
    void load_FallsBackToPropertiesFile() {
        System.clearProperty("db.url");

        AppConfig config = AppConfig.load(NO_ENV);

        assertEquals("jdbc:postgresql://localhost:5432/recetea_test", config.dbUrl(),
                "Without an override, dbUrl must come from application-test.properties");
    }

    @Test
    @DisplayName("System property overrides the value in application-test.properties")
    void load_SystemPropertyOverridesPropertiesFile() {
        String override = "jdbc:postgresql://override-host:5432/override_db";
        System.setProperty("db.url", override);

        AppConfig config = AppConfig.load(NO_ENV);

        assertEquals(override, config.dbUrl(),
                "System property must take precedence over the properties file");
        // Other properties (no override set) still come from the file
        assertEquals("postgres", config.dbUser());
    }

    @Test
    @DisplayName("Env-var lookup overrides system property AND properties file")
    void load_EnvLookupWinsTopOfPrecedenceChain() {
        // Set sys prop to verify it loses to the env tier
        System.setProperty("db.url", "jdbc:postgresql://sysprop-host:5432/sysprop_db");
        Function<String, String> envWithDbUrl = Map.of(
                "DB_URL", "jdbc:postgresql://env-host:5432/env_db"
        )::get;

        AppConfig config = AppConfig.load(envWithDbUrl);

        assertEquals("jdbc:postgresql://env-host:5432/env_db", config.dbUrl(),
                "Env-var tier must win over sys-prop and properties file");
    }

    @Test
    @DisplayName("Default fallbacks fire when no override exists for optional ints")
    void load_AppliesDefaultsForOptionalIntFields() {
        System.clearProperty("db.url");

        AppConfig config = AppConfig.load(NO_ENV);

        // application-test.properties does not set db.maxPoolSize / db.connectionTimeout
        assertEquals(10,     config.maxPoolSize(),       "Default max pool size should be 10");
        assertEquals(20_000, config.connectionTimeout(),
                "Default connection timeout should be 20s — covers Neon/Supabase cold-start budgets");
    }

    @Test
    @DisplayName("Cloud SSL mode and CDN URL are absent by default and surface as null")
    void load_OptionalCloudFieldsDefaultToNull() {
        System.clearProperty("db.sslMode");
        System.clearProperty("media.cdnUrl");

        AppConfig config = AppConfig.load(NO_ENV);

        assertNull(config.dbSslMode(),
                "dbSslMode should be null when neither env, sysprop, nor properties file sets it");
        assertNull(config.mediaCdnUrl(),
                "mediaCdnUrl should be null when neither env, sysprop, nor properties file sets it");
    }

    @Test
    @DisplayName("System-property overrides reach the new cloud fields")
    void load_CloudFieldsRespectPrecedenceChain() {
        System.setProperty("db.sslMode",   "require");
        System.setProperty("media.cdnUrl", "https://cdn.example.com");

        AppConfig config = AppConfig.load(NO_ENV);

        assertEquals("require",                  config.dbSslMode(),
                "dbSslMode should reflect the system-property override");
        assertEquals("https://cdn.example.com",  config.mediaCdnUrl(),
                "mediaCdnUrl should reflect the system-property override");
    }
}
