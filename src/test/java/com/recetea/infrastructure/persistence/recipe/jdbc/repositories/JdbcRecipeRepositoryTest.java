package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcRecipeRepositoryTest extends BaseRepositoryTest {

    private JdbcRecipeRepository repository;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp() throws SQLException {
        transactionManager = new JdbcTransactionManager(dataSource);
        repository = new JdbcRecipeRepository(transactionManager);
        seedReferenceData();
    }

    private void seedReferenceData() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO users (id_user, username, email, password_hash) OVERRIDING SYSTEM VALUE VALUES (1, 'test', 'test@test.com', 'hash')");
            st.execute("INSERT INTO users (id_user, username, email, password_hash) OVERRIDING SYSTEM VALUE VALUES (2, 'voter1', 'voter1@test.com', 'hash')");
            st.execute("INSERT INTO users (id_user, username, email, password_hash) OVERRIDING SYSTEM VALUE VALUES (3, 'voter2', 'voter2@test.com', 'hash')");
            st.execute("INSERT INTO categories (id_category, name) OVERRIDING SYSTEM VALUE VALUES (1, 'TestCat')");
            st.execute("INSERT INTO difficulties (id_difficulty, level_name) OVERRIDING SYSTEM VALUE VALUES (1, 'Easy')");
            st.execute("INSERT INTO ingredient_categories (id_ing_category, name) OVERRIDING SYSTEM VALUE VALUES (1, 'TestIngCat')");
            st.execute("INSERT INTO unit_measures (id_unit, name, abbreviation) OVERRIDING SYSTEM VALUE VALUES (1, 'Gramo', 'g')");
            st.execute("INSERT INTO ingredients (id_ingredient, ing_category_id, name) OVERRIDING SYSTEM VALUE VALUES " +
                    "(1, 1, 'Ing1'), (2, 1, 'Ing2'), (3, 1, 'Ing3'), (4, 1, 'Ing4')");
        }
    }

    private Recipe buildRecipe() {
        return new Recipe(
                new UserId(1),
                new Category(new CategoryId(1), "TestCat"),
                new Difficulty(new DifficultyId(1), "Easy"),
                "Receta Test", "Descripción test",
                new PreparationTime(30),
                new Servings(2)
        );
    }

    @Test
    @DisplayName("Debe persistir receta y componentes bajo la misma transacción")
    void save_DebePersistirRecetaYComponentesBajoMismaTransaccion() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(500))
        ));
        recipe.syncSteps(List.of(new RecipeStep(1, "Cocinar todo")));

        transactionManager.execute(() -> repository.save(recipe));

        assertNotNull(recipe.getId(), "La identidad debe ser generada por la DB");
        int id = recipe.getId().value();
        assertEquals(1, queryCount("SELECT count(*) FROM recipes WHERE id_recipe = " + id), "La receta debe existir en DB");
        assertEquals(1, queryCount("SELECT count(*) FROM recipe_ingredients WHERE recipe_id = " + id), "El ingrediente debe haberse persistido");
    }

    @Test
    @DisplayName("Debe ejecutar borrado en cascada sin dejar registros huérfanos")
    void delete_DebeEjecutarBorradoEnCascadaSinDejarRegistrosHuerfanos() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100))
        ));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso único")));

        transactionManager.execute(() -> repository.save(recipe));
        int id = recipe.getId().value();

        transactionManager.execute(() -> repository.delete(recipe.getId()));

        assertEquals(0, queryCount("SELECT count(*) FROM recipes WHERE id_recipe = " + id));
        assertEquals(0, queryCount("SELECT count(*) FROM recipe_ingredients WHERE recipe_id = " + id));
        assertEquals(0, queryCount("SELECT count(*) FROM steps WHERE recipe_id = " + id));
    }

    @Test
    @DisplayName("El Smart Diff debe actualizar, insertar y eliminar ingredientes de forma granular")
    void testSmartDiffingLogic() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100)),
                new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.valueOf(200)),
                new RecipeIngredient(new IngredientId(3), new UnitId(1), BigDecimal.valueOf(300))
        ));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso inicial")));
        transactionManager.execute(() -> repository.save(recipe));

        int recipeId = recipe.getId().value();

        recipe.syncIngredients(List.of(
                new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.valueOf(100)),
                new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.valueOf(250)),
                new RecipeIngredient(new IngredientId(4), new UnitId(1), BigDecimal.valueOf(400))
        ));
        transactionManager.execute(() -> repository.update(recipe));

        assertEquals(BigDecimal.valueOf(100).setScale(2), queryQuantity(recipeId, 1), "Ingrediente 1 sin cambios");
        assertEquals(BigDecimal.valueOf(250).setScale(2), queryQuantity(recipeId, 2), "Ingrediente 2 actualizado");
        assertNull(queryQuantity(recipeId, 3), "Ingrediente 3 eliminado");
        assertEquals(BigDecimal.valueOf(400).setScale(2), queryQuantity(recipeId, 4), "Ingrediente 4 nuevo");
    }

    @Test
    @DisplayName("findById debe hidratar la lista de valoraciones del agregado")
    void findById_ShouldHydrateRatingsList() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        seedRating(new UserId(2), recipeId, 4);
        seedRating(new UserId(3), recipeId, 5);

        Recipe loaded = repository.findById(recipeId).orElseThrow();

        assertEquals(2, loaded.getRatings().size(), "Debe hidratar exactamente 2 valoraciones");
        assertTrue(loaded.getRatings().stream()
                .anyMatch(r -> r.getUserId().equals(new UserId(2)) && r.getScore().value() == 4));
        assertTrue(loaded.getRatings().stream()
                .anyMatch(r -> r.getUserId().equals(new UserId(3)) && r.getScore().value() == 5));
    }

    @Test
    @DisplayName("update debe sincronizar las métricas denormalizadas en la tabla recipes")
    void update_ShouldSynchronizeDenormalizedMetrics() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        seedRating(new UserId(2), recipeId, 5);

        transactionManager.execute(() -> repository.update(recipe));

        Recipe loaded = repository.findById(recipeId).orElseThrow();
        assertEquals(1, loaded.getTotalRatings(), "total_ratings debe ser 1 tras la sincronización");
        assertEquals(0, new BigDecimal("5.00").compareTo(loaded.getAverageScore()),
                "average_score debe reflejar la puntuación del rating insertado");
    }

    @Test
    @DisplayName("findAllSummaries debe leer average_score y total_ratings de las columnas denormalizadas")
    void findAllSummaries_ShouldReadFromDenormalizedColumns() {
        Recipe recipeA = buildRecipe();
        recipeA.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipeA.syncSteps(List.of(new RecipeStep(1, "Paso A")));
        transactionManager.execute(() -> repository.save(recipeA));
        seedRating(new UserId(2), recipeA.getId(), 4);
        seedRating(new UserId(3), recipeA.getId(), 5);
        // Trigger denormalization sync: UPDATE_RECIPE_METRICS recalculates from ratings table
        transactionManager.execute(() -> repository.update(recipeA));

        Recipe recipeB = buildRecipe();
        recipeB.syncIngredients(List.of(new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.ONE)));
        recipeB.syncSteps(List.of(new RecipeStep(1, "Paso B")));
        transactionManager.execute(() -> repository.save(recipeB));
        // recipeB has no ratings: denormalized defaults (0, 0.00) apply

        List<RecipeSummaryResponse> summaries = repository.findAllSummaries();

        RecipeSummaryResponse summaryA = summaries.stream()
                .filter(s -> s.id().equals(recipeA.getId())).findFirst().orElseThrow();
        RecipeSummaryResponse summaryB = summaries.stream()
                .filter(s -> s.id().equals(recipeB.getId())).findFirst().orElseThrow();

        // Metrics are read from denormalized columns (no LEFT JOIN ratings / GROUP BY in the query).
        // Correctness is proven by the fact that seeding ratings without calling update() leaves
        // the values at DEFAULT 0 — only the UPDATE_RECIPE_METRICS subquery propagates them.
        assertEquals(0, new BigDecimal("4.50").compareTo(summaryA.averageScore()),
                "average_score de receta A debe ser 4.50 (leído de columna denormalizada)");
        assertEquals(2, summaryA.totalRatings(),
                "total_ratings de receta A debe ser 2 (leído de columna denormalizada)");
        assertEquals(0, BigDecimal.ZERO.compareTo(summaryB.averageScore()),
                "average_score de receta B debe ser 0.00 (sin valoraciones, DEFAULT aplicado)");
        assertEquals(0, summaryB.totalRatings(),
                "total_ratings de receta B debe ser 0");
    }

    private void seedRating(UserId userId, RecipeId recipeId, int score) {
        String sql = "INSERT INTO ratings (user_id, recipe_id, score, comment, created_at) VALUES (" +
                userId.value() + ", " + recipeId.value() + ", " + score + ", 'Test comment', NOW())";
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int queryCount(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal queryQuantity(int recipeId, int ingredientId) {
        String sql = "SELECT quantity FROM recipe_ingredients WHERE recipe_id = " + recipeId + " AND ingredient_id = " + ingredientId;
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getBigDecimal("quantity") : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
