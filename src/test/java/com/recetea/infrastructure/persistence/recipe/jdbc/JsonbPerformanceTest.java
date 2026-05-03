package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseRepositoryTest;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcRecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark for the {@code SELECT_FULL_AGGREGATE} query — a single
 * round-trip that pulls a recipe plus four LATERAL JSONB aggregations
 * (ingredients, steps, ratings, media) and rehydrates them client-side.
 *
 * <p>Workload: 1 recipe with 50 ingredients, 50 steps, and 1,000 ratings. The
 * ratings list is the dominant payload — at ~150 bytes per rating in the JSONB
 * shape, the {@code ratings_json} column carries roughly 150 KB across the wire.
 *
 * <p>The 100ms budget is the local-developer SLA: anything slower indicates the
 * single-query design has lost its advantage over an N+1 hydration approach.
 * Sustained measurement (50 iterations) catches GC-induced jitter that a
 * single-shot timing would miss.
 */
@DisplayName("JSONB aggregation hydration — performance under heavy aggregate load")
class JsonbPerformanceTest extends BaseRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(JsonbPerformanceTest.class);

    private static final int  INGREDIENTS  = 50;
    private static final int  STEPS        = 50;
    private static final int  RATINGS      = 1_000;
    private static final long BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private JdbcRecipeRepository repository;
    private RecipeId recipeId;

    @BeforeEach
    void seedHeavyAggregate() throws SQLException {
        repository = new JdbcRecipeRepository(new JdbcTransactionManager(dataSource), metricsPort);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                seedReferenceData(conn);
                seedUsers(conn);
                seedIngredientCatalogue(conn);
                seedRecipe(conn);
                seedRecipeIngredients(conn);
                seedRecipeSteps(conn);
                seedRatings(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        recipeId = new RecipeId(1);
    }

    private void seedReferenceData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO categories (category_id, name) OVERRIDING SYSTEM VALUE VALUES (1, 'PerfCat')");
            st.execute("INSERT INTO difficulties (difficulty_id, difficulty_level) OVERRIDING SYSTEM VALUE VALUES (1, 'PerfDiff')");
            st.execute("INSERT INTO ingredient_categories (ingredient_category_id, name) OVERRIDING SYSTEM VALUE VALUES (1, 'PerfIngCat')");
            st.execute("INSERT INTO unit_measures (unit_id, name, abbreviation) OVERRIDING SYSTEM VALUE VALUES (1, 'PerfUnit', 'pu')");
        }
    }

    private void seedUsers(Connection conn) throws SQLException {
        // user_id=1 is the recipe owner; ids 2..RATINGS+1 are voters (1000 of them)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (user_id, username, email, password_hash) " +
                "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, 'hash')")) {
            for (int i = 1; i <= RATINGS + 1; i++) {
                ps.setInt(1, i);
                ps.setString(2, "user-" + i);
                ps.setString(3, "user-" + i + "@perf.test");
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedIngredientCatalogue(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ingredients (ingredient_id, ingredient_category_id, name) " +
                "OVERRIDING SYSTEM VALUE VALUES (?, 1, ?)")) {
            for (int i = 1; i <= INGREDIENTS; i++) {
                ps.setInt(1, i);
                ps.setString(2, "ing-" + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedRecipe(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO recipes (recipe_id, user_id, category_id, difficulty_id, " +
                    "title, description, prep_time_min, servings) OVERRIDING SYSTEM VALUE " +
                    "VALUES (1, 1, 1, 1, 'Perf Recipe', 'Heavy aggregate test', 30, 4)");
        }
    }

    private void seedRecipeIngredients(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (1, ?, 1, ?)")) {
            for (int i = 1; i <= INGREDIENTS; i++) {
                ps.setInt(1, i);
                ps.setBigDecimal(2, java.math.BigDecimal.valueOf(i * 10));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedRecipeSteps(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO steps (recipe_id, step_order, instruction) VALUES (1, ?, ?)")) {
            for (int i = 1; i <= STEPS; i++) {
                ps.setInt(1, i);
                ps.setString(2, "Instruction for step " + i + " — perf benchmark filler.");
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedRatings(Connection conn) throws SQLException {
        // Voter user ids start at 2 (1 is the owner; trigger forbids self-rating)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ratings (user_id, recipe_id, score, comment, created_at) " +
                "VALUES (?, 1, ?, ?, NOW())")) {
            for (int i = 2; i <= RATINGS + 1; i++) {
                ps.setInt(1, i);
                ps.setInt(2, (i % 5) + 1);
                ps.setString(3, "Rating comment " + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Test
    @DisplayName("findById on 1000-rating aggregate stays under 100ms with stable sustained performance")
    void heavyAggregateFetch_StaysWithinBudget() {
        // Warmup: prime JIT, JSONB parser, connection-pool slot, RecipeMapper class init.
        for (int i = 0; i < 3; i++) {
            Recipe r = repository.findById(recipeId).orElseThrow();
            assertEquals(INGREDIENTS, r.getIngredients().size(), "warmup ingredient count");
            assertEquals(STEPS,       r.getSteps().size(),       "warmup step count");
            assertEquals(RATINGS,     r.getRatings().size(),     "warmup rating count");
        }

        // Headline number: a single fetch after warmup.
        long singleStart = System.nanoTime();
        Recipe recipe = repository.findById(recipeId).orElseThrow();
        long singleNanos = System.nanoTime() - singleStart;

        assertEquals(INGREDIENTS, recipe.getIngredients().size());
        assertEquals(STEPS,       recipe.getSteps().size());
        assertEquals(RATINGS,     recipe.getRatings().size());

        // Sustained throughput: 50 fetches average. Catches GC pressure and
        // any per-call resource leakage that would compound over iterations.
        int iterations = 50;
        long sustainedStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Recipe r = repository.findById(recipeId).orElseThrow();
            // Touch the lists so JIT can't dead-code-eliminate the rehydration work.
            assertEquals(RATINGS, r.getRatings().size());
        }
        long sustainedNanos = System.nanoTime() - sustainedStart;
        long avgNanos = sustainedNanos / iterations;

        long singleMs = TimeUnit.NANOSECONDS.toMillis(singleNanos);
        long avgMs    = TimeUnit.NANOSECONDS.toMillis(avgNanos);
        log.info("JSONB aggregate fetch — single: {}ms | sustained avg over {} iters: {}ms",
                singleMs, iterations, avgMs);

        assertTrue(singleNanos < BUDGET_NANOS,
                "Single findById on the heavy aggregate should be < 100ms; actual: " + singleMs + "ms");
        assertTrue(avgNanos < BUDGET_NANOS,
                "Sustained average should stay < 100ms across " + iterations + " calls; actual avg: " + avgMs + "ms");
    }
}
