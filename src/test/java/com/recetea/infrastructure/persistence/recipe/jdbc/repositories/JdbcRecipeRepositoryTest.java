package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.vo.Score;
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
    @DisplayName("updateSocialMetrics debe persistir las métricas calculadas por el dominio")
    void updateSocialMetrics_ShouldPersistDomainMetrics() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        recipe.setSocialMetrics(new BigDecimal("5.00"), 1);
        transactionManager.execute(() ->
                repository.updateSocialMetrics(recipeId, recipe.getAverageScore(), recipe.getTotalRatings()));

        Recipe loaded = repository.findById(recipeId).orElseThrow();
        assertEquals(1, loaded.getTotalRatings(), "total_ratings debe ser 1 tras la sincronización");
        assertEquals(0, new BigDecimal("5.00").compareTo(loaded.getAverageScore()),
                "average_score debe reflejar la puntuación del rating insertado");
    }

    @Test
    @DisplayName("update debe persistir métricas automáticamente cuando addRating marca el agregado como dirty")
    void update_ShouldPersistMetrics_WhenAddRatingMarksAggregateDirty() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        Recipe loaded = repository.findById(recipeId).orElseThrow();
        loaded.addRating(new UserId(2), new Score(4), "Muy buena receta");

        assertTrue(loaded.isMetricsDirty(), "El agregado debe estar marcado como dirty tras addRating");
        transactionManager.execute(() -> repository.update(loaded));

        assertFalse(loaded.isMetricsDirty(), "El flag dirty debe limpiarse tras persistir");

        BigDecimal persistedScore = queryBigDecimal(
                "SELECT average_score FROM recipes WHERE id_recipe = " + recipeId.value());
        int persistedTotal = queryCount(
                "SELECT total_ratings FROM recipes WHERE id_recipe = " + recipeId.value());
        int ratingRows = queryCount(
                "SELECT count(*) FROM ratings WHERE recipe_id = " + recipeId.value());

        assertEquals(0, new BigDecimal("4.00").compareTo(persistedScore),
                "average_score debe ser 4.00 (único voto de 4)");
        assertEquals(1, persistedTotal, "total_ratings debe ser 1");
        assertEquals(1, ratingRows, "La valoración debe haber sido insertada en la tabla ratings");
    }

    @Test
    @DisplayName("update NO debe tocar la tabla ratings cuando el agregado no está dirty")
    void update_ShouldNotPersistRatings_WhenAggregatIsNotDirty() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));

        seedRating(new UserId(2), recipe.getId(), 5);

        Recipe loaded = repository.findById(recipe.getId()).orElseThrow();
        loaded.setTitle("Título Actualizado");
        assertFalse(loaded.isMetricsDirty(), "No dirty: ninguna valoración nueva añadida en memoria");

        transactionManager.execute(() -> repository.update(loaded));

        assertEquals(1, queryCount("SELECT count(*) FROM ratings WHERE recipe_id = " + recipe.getId().value()),
                "El número de filas en ratings no debe cambiar si no hay nuevas valoraciones");
    }

    @Test
    @DisplayName("findAllSummaries debe leer average_score y total_ratings de las columnas denormalizadas")
    void findAllSummaries_ShouldReadFromDenormalizedColumns() {
        Recipe recipeA = buildRecipe();
        recipeA.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipeA.syncSteps(List.of(new RecipeStep(1, "Paso A")));
        transactionManager.execute(() -> repository.save(recipeA));
        // Push domain-computed metrics to the denormalized columns (simulates two ratings: avg 4.50)
        recipeA.setSocialMetrics(new BigDecimal("4.50"), 2);
        transactionManager.execute(() ->
                repository.updateSocialMetrics(recipeA.getId(), recipeA.getAverageScore(), recipeA.getTotalRatings()));

        Recipe recipeB = buildRecipe();
        recipeB.syncIngredients(List.of(new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.ONE)));
        recipeB.syncSteps(List.of(new RecipeStep(1, "Paso B")));
        transactionManager.execute(() -> repository.save(recipeB));
        // recipeB has no ratings: denormalized defaults (0, 0.00) apply

        List<RecipeSummaryResponse> summaries = repository.findAllSummaries(new PageRequest(0, 100)).content();

        RecipeSummaryResponse summaryA = summaries.stream()
                .filter(s -> s.id().equals(recipeA.getId())).findFirst().orElseThrow();
        RecipeSummaryResponse summaryB = summaries.stream()
                .filter(s -> s.id().equals(recipeB.getId())).findFirst().orElseThrow();

        // Correctness is proven by the fact that seeding ratings directly into DB without calling
        // updateSocialMetrics leaves the denormalized columns unchanged — summaries read them directly.
        assertEquals(0, new BigDecimal("4.50").compareTo(summaryA.averageScore()),
                "average_score de receta A debe ser 4.50 (leído de columna denormalizada)");
        assertEquals(2, summaryA.totalRatings(),
                "total_ratings de receta A debe ser 2 (leído de columna denormalizada)");
        assertEquals(0, BigDecimal.ZERO.compareTo(summaryB.averageScore()),
                "average_score de receta B debe ser 0.00 (sin valoraciones, DEFAULT aplicado)");
        assertEquals(0, summaryB.totalRatings(),
                "total_ratings de receta B debe ser 0");
    }

    @Test
    @DisplayName("findAllSummaries debe retornar la página solicitada y el total correcto")
    void findAllSummaries_ShouldReturnCorrectPageAndTotalElements() {
        for (int i = 1; i <= 5; i++) {
            Recipe r = buildRecipe();
            r.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
            r.syncSteps(List.of(new RecipeStep(1, "Paso " + i)));
            transactionManager.execute(() -> repository.save(r));
        }

        PageResponse<RecipeSummaryResponse> page = repository.findAllSummaries(new PageRequest(0, 2));

        assertEquals(2, page.content().size(), "La primera página debe contener 2 recetas");
        assertEquals(5L, page.totalElements(), "El total debe ser 5");
        assertEquals(3, page.totalPages(), "Deben existir 3 páginas para 5 elementos con tamaño 2");
    }

    @Test
    @DisplayName("findAllSummaries segunda página debe traer el resto de registros")
    void findAllSummaries_SecondPage_ShouldReturnRemainingRecords() {
        for (int i = 1; i <= 5; i++) {
            Recipe r = buildRecipe();
            r.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
            r.syncSteps(List.of(new RecipeStep(1, "Paso " + i)));
            transactionManager.execute(() -> repository.save(r));
        }

        PageResponse<RecipeSummaryResponse> lastPage = repository.findAllSummaries(new PageRequest(2, 2));

        assertEquals(1, lastPage.content().size(), "La última página debe contener el único registro restante");
        assertEquals(5L, lastPage.totalElements());
    }

    @Test
    @DisplayName("hasUserRatedRecipe debe retornar true solo cuando existe una valoración del usuario para esa receta")
    void hasUserRatedRecipe_ShouldReturnTrueOnlyWhenRatingExists() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        assertFalse(repository.hasUserRatedRecipe(new UserId(2), recipeId),
                "Antes de votar debe retornar false");

        seedRating(new UserId(2), recipeId, 4);

        assertTrue(repository.hasUserRatedRecipe(new UserId(2), recipeId),
                "Tras votar debe retornar true");
        assertFalse(repository.hasUserRatedRecipe(new UserId(3), recipeId),
                "Un usuario distinto sin voto debe retornar false");
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

    private BigDecimal queryBigDecimal(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getBigDecimal(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("findById debe hidratar la lista de recursos multimedia del agregado")
    void findById_ShouldHydrateMediaList() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        recipe.addMedia(new RecipeMedia(null, recipeId, "key/main.jpg", "LOCAL", "image/jpeg", 204800L, true,  0));
        recipe.addMedia(new RecipeMedia(null, recipeId, "key/extra.png", "LOCAL", "image/png",  102400L, false, 1));
        transactionManager.execute(() -> repository.update(recipe));

        Recipe loaded = repository.findById(recipeId).orElseThrow();

        assertEquals(2, loaded.getMediaItems().size(), "Debe hidratar exactamente 2 recursos multimedia");
        RecipeMedia main = loaded.getMediaItems().stream().filter(RecipeMedia::isMain).findFirst().orElseThrow();
        RecipeMedia extra = loaded.getMediaItems().stream().filter(m -> !m.isMain()).findFirst().orElseThrow();

        assertEquals("key/main.jpg", main.storageKey());
        assertEquals("LOCAL", main.storageProvider());
        assertEquals("image/jpeg", main.mimeType());
        assertEquals(204800L, main.sizeBytes());
        assertEquals(0, main.sortOrder());

        assertEquals("key/extra.png", extra.storageKey());
        assertEquals(1, extra.sortOrder());
    }

    @Test
    @DisplayName("syncMedia debe aplicar smart-diff: insertar, actualizar y eliminar recursos multimedia")
    void update_ShouldSmartDiffMedia() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        recipe.addMedia(new RecipeMedia(null, recipeId, "key/a.jpg", "LOCAL", "image/jpeg", 100L, true,  0));
        recipe.addMedia(new RecipeMedia(null, recipeId, "key/b.jpg", "LOCAL", "image/jpeg", 200L, false, 1));
        transactionManager.execute(() -> repository.update(recipe));

        Recipe afterFirst = repository.findById(recipeId).orElseThrow();
        assertEquals(2, afterFirst.getMediaItems().size());

        RecipeMediaId idA = afterFirst.getMediaItems().stream().filter(RecipeMedia::isMain).findFirst().orElseThrow().id();
        RecipeMediaId idB = afterFirst.getMediaItems().stream().filter(m -> !m.isMain()).findFirst().orElseThrow().id();

        // Swap main + add new item, remove B
        afterFirst.setMainMedia(idB);
        afterFirst.removeMedia(idA);
        afterFirst.addMedia(new RecipeMedia(null, recipeId, "key/c.jpg", "LOCAL", "image/png", 300L, false, 2));
        transactionManager.execute(() -> repository.update(afterFirst));

        Recipe final_ = repository.findById(recipeId).orElseThrow();

        assertEquals(2, final_.getMediaItems().size(), "Debe quedar 2 elementos tras el diff");
        assertTrue(final_.getMediaItems().stream().anyMatch(m -> "key/b.jpg".equals(m.storageKey()) && m.isMain()),
                "key/b.jpg debe ser ahora main");
        assertTrue(final_.getMediaItems().stream().anyMatch(m -> "key/c.jpg".equals(m.storageKey())),
                "key/c.jpg debe haber sido insertado");
        assertFalse(final_.getMediaItems().stream().anyMatch(m -> "key/a.jpg".equals(m.storageKey())),
                "key/a.jpg debe haber sido eliminado");
    }

    @Test
    @DisplayName("findAllSummaries debe retornar el mainMediaStorageKey correcto cuando la receta tiene imagen principal")
    void findAllSummaries_ShouldReturnMainMediaStorageKey_WhenRecipeHasMainImage() {
        Recipe recipe = buildRecipe();
        recipe.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        recipe.syncSteps(List.of(new RecipeStep(1, "Paso 1")));
        transactionManager.execute(() -> repository.save(recipe));
        RecipeId recipeId = recipe.getId();

        recipe.addMedia(new RecipeMedia(null, recipeId, "media/thumbnail.jpg", "LOCAL", "image/jpeg", 51200L, true, 0));
        transactionManager.execute(() -> repository.update(recipe));

        List<RecipeSummaryResponse> summaries = repository.findAllSummaries(new PageRequest(0, 100)).content();
        RecipeSummaryResponse summary = summaries.stream()
                .filter(s -> s.id().equals(recipeId)).findFirst().orElseThrow();

        assertEquals("media/thumbnail.jpg", summary.mainMediaStorageKey(),
                "mainMediaStorageKey debe coincidir con el storage_key de la imagen principal");
    }

    @Test
    @DisplayName("findAllSummaries debe incluir recetas sin imagen (LEFT JOIN: mainMediaStorageKey nulo)")
    void findAllSummaries_ShouldIncludeRecipesWithoutMedia_WithNullStorageKey() {
        Recipe withMedia = buildRecipe();
        withMedia.syncIngredients(List.of(new RecipeIngredient(new IngredientId(1), new UnitId(1), BigDecimal.ONE)));
        withMedia.syncSteps(List.of(new RecipeStep(1, "Paso A")));
        transactionManager.execute(() -> repository.save(withMedia));
        withMedia.addMedia(new RecipeMedia(null, withMedia.getId(), "media/img.jpg", "LOCAL", "image/jpeg", 1024L, true, 0));
        transactionManager.execute(() -> repository.update(withMedia));

        Recipe noMedia = buildRecipe();
        noMedia.syncIngredients(List.of(new RecipeIngredient(new IngredientId(2), new UnitId(1), BigDecimal.ONE)));
        noMedia.syncSteps(List.of(new RecipeStep(1, "Paso B")));
        transactionManager.execute(() -> repository.save(noMedia));

        List<RecipeSummaryResponse> summaries = repository.findAllSummaries(new PageRequest(0, 100)).content();

        RecipeSummaryResponse withMediaSummary = summaries.stream()
                .filter(s -> s.id().equals(withMedia.getId())).findFirst().orElseThrow();
        RecipeSummaryResponse noMediaSummary = summaries.stream()
                .filter(s -> s.id().equals(noMedia.getId())).findFirst().orElseThrow();

        assertEquals("media/img.jpg", withMediaSummary.mainMediaStorageKey(),
                "La receta con imagen debe exponer su storage_key");
        assertNull(noMediaSummary.mainMediaStorageKey(),
                "La receta sin imagen debe retornar null (LEFT JOIN no produce fila)");
        assertEquals(2L, summaries.stream()
                .filter(s -> s.id().equals(withMedia.getId()) || s.id().equals(noMedia.getId())).count(),
                "Ambas recetas deben aparecer en el listado");
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
