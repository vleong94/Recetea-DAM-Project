package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.shared.domain.utils.MaskingUtils;
import com.recetea.infrastructure.metrics.NoOpMetricsAdapter;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.AppConfig;
import com.recetea.infrastructure.persistence.recipe.jdbc.config.DatabaseConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for persistence integration tests.
 * Guarantees a pure state in the test database by performing a bulk truncation
 * before each test method, ensuring predictable results and no state leakage.
 */
public abstract class BaseRepositoryTest {

    protected static DataSource dataSource;

    /**
     * Shared metrics port for the repositories every subclass test wires up. Initialised
     * once in {@link #setupEnvironment} (mirrors the {@code dataSource} field's lazy-static
     * pattern) so the NoOp adapter is constructed alongside the rest of the test fixture
     * rather than at class load. The NoOp implementation discards every call — it satisfies
     * {@link BaseJdbcRepository}'s constructor null-check without imposing logging side
     * effects on the test suite.
     */
    protected static IMetricsPort metricsPort;

    @BeforeAll
    static void setupEnvironment() {
        // Pin the integration suite to application-test.properties.
        //
        // env=test selects which file AppConfig loads
        // (application-<env>.properties); loadForTesting() neutralises the
        // env-var tier of AppConfig's precedence chain so a DB_URL / DB_USER
        // / DB_PASSWORD exported in the developer's shell or IDE run config
        // (typically pointing at the production Supabase instance for normal
        // app launches) cannot bleed into tests. This is what keeps the
        // @BeforeEach TRUNCATE … RESTART IDENTITY CASCADE local — without it,
        // the env vars would silently route the wipe at production data
        // (the tell-tale symptom: "only user 'ana' (id=1) survives" because
        // JdbcUserRepositoryTest inserts that fixture after the wipe).
        System.setProperty("env", "test");
        AppConfig config = AppConfig.loadForTesting();

        // Defence-in-depth: even with loadForTesting(), refuse to proceed if
        // the resolved URL doesn't look local. Catches the case where someone
        // edits application-test.properties to point at a remote DB, or sets
        // -Ddb.url=… as a system-property override.
        String url = config.dbUrl();
        if (url == null || !(url.contains("localhost") || url.contains("127.0.0.1"))) {
            throw new IllegalStateException(
                    "REFUSING to run integration tests against a non-local database.\n"
                  + "  Resolved JDBC URL: " + MaskingUtils.maskJdbcUrl(url) + "\n"
                  + "  Required: URL must contain 'localhost' or '127.0.0.1'.\n"
                  + "  Check application-test.properties and any -Ddb.url system property.\n"
                  + "  Expected: jdbc:postgresql://localhost:5432/recetea_test");
        }

        dataSource = DatabaseConfig.getDataSource(config);
        metricsPort = new NoOpMetricsAdapter();
    }

    @BeforeEach
    void cleanDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Atomic purge of all tables respecting the foreign key hierarchy
            stmt.execute("TRUNCATE TABLE " +
                    "ratings, favorites, recipe_tags, recipe_media, steps, recipe_ingredients, " +
                    "recipes, ingredients, unit_measures, ingredient_categories, " +
                    "difficulties, categories, tags, users " +
                    "RESTART IDENTITY CASCADE");

        } catch (SQLException e) {
            throw new RuntimeException("Critical failure: Could not reset the test environment.", e);
        }
    }
}
