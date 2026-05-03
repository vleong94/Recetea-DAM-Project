package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JdbcRecipeMediaRepository — Media resource persistence")
class JdbcRecipeMediaRepositoryTest extends BaseRepositoryTest {

    private JdbcRecipeMediaRepository repository;
    private JdbcTransactionManager transactionManager;

    private static final RecipeId RECIPE_ID = new RecipeId(1);

    @BeforeEach
    void setUp() throws SQLException {
        transactionManager = new JdbcTransactionManager(dataSource);
        repository = new JdbcRecipeMediaRepository(transactionManager, metricsPort);
        seedReferenceData();
    }

    private void seedReferenceData() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO users (user_id, username, email, password_hash) OVERRIDING SYSTEM VALUE VALUES (1, 'author', 'author@test.com', 'hash')");
            st.execute("INSERT INTO categories (category_id, name) OVERRIDING SYSTEM VALUE VALUES (1, 'TestCat')");
            st.execute("INSERT INTO difficulties (difficulty_id, difficulty_level) OVERRIDING SYSTEM VALUE VALUES (1, 'Easy')");
            st.execute("INSERT INTO recipes (recipe_id, user_id, category_id, difficulty_id, title, prep_time_min, servings) OVERRIDING SYSTEM VALUE VALUES (1, 1, 1, 1, 'Receta Test', 30, 2)");
        }
    }

    private RecipeMedia buildMedia(boolean isMain, int sortOrder) {
        return new RecipeMedia(
                null,
                RECIPE_ID,
                "media/2024/abc123.jpg",
                "LOCAL",
                "image/jpeg",
                204800L,
                isMain,
                sortOrder);
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save: Should persist media and return the entity with a generated ID")
    void save_ShouldPersistAndReturnEntityWithGeneratedId() {
        RecipeMedia saved = transactionManager.execute(() -> repository.save(buildMedia(true, 0)));

        assertNotNull(saved.id(), "The ID must have been assigned by the database");
        assertTrue(saved.id().value() > 0);
        assertEquals(RECIPE_ID, saved.recipeId());
        assertEquals("media/2024/abc123.jpg", saved.storageKey());
        assertEquals("LOCAL", saved.storageProvider());
        assertEquals("image/jpeg", saved.mimeType());
        assertEquals(204800L, saved.sizeBytes());
        assertTrue(saved.isMain());
        assertEquals(0, saved.sortOrder());
    }

    @Test
    @DisplayName("save: Should persist all metadata columns to the database")
    void save_ShouldPersistAllMetadataColumns() throws SQLException {
        RecipeMedia saved = transactionManager.execute(() -> repository.save(buildMedia(false, 2)));

        String sql = "SELECT storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
                     "FROM recipe_media WHERE media_id = " + saved.id().value();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "Row must exist in the DB");
            assertEquals("media/2024/abc123.jpg", rs.getString("storage_key"));
            assertEquals("LOCAL", rs.getString("storage_provider"));
            assertEquals("image/jpeg", rs.getString("mime_type"));
            assertEquals(204800L, rs.getLong("size_bytes"));
            assertFalse(rs.getBoolean("is_main"));
            assertEquals(2, rs.getInt("sort_order"));
        }
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findById: Should correctly reconstruct the entity from the database")
    void findById_ShouldReconstructEntityWithAllFields() {
        RecipeMedia saved = transactionManager.execute(() -> repository.save(buildMedia(true, 0)));

        Optional<RecipeMedia> found = repository.findById(saved.id());

        assertTrue(found.isPresent(), "The entity must be found by ID");
        RecipeMedia media = found.get();
        assertEquals(saved.id(), media.id());
        assertEquals(RECIPE_ID, media.recipeId());
        assertEquals("media/2024/abc123.jpg", media.storageKey());
        assertEquals("LOCAL", media.storageProvider());
        assertEquals("image/jpeg", media.mimeType());
        assertEquals(204800L, media.sizeBytes());
        assertTrue(media.isMain());
        assertEquals(0, media.sortOrder());
    }

    @Test
    @DisplayName("findById: Should return an empty Optional when the ID does not exist")
    void findById_ShouldReturnEmpty_WhenNotFound() {
        Optional<RecipeMedia> result = repository.findById(new RecipeMediaId(9999));

        assertTrue(result.isEmpty(), "Optional must be empty for a non-existent ID");
    }

    // -------------------------------------------------------------------------
    // findByRecipeId
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByRecipeId: Should return all media ordered by sort_order")
    void findByRecipeId_ShouldReturnAllMediaOrderedBySortOrder() {
        transactionManager.execute(() -> {
            repository.save(new RecipeMedia(null, RECIPE_ID, "key/c.jpg", "LOCAL", "image/jpeg", 100L, false, 2));
            repository.save(new RecipeMedia(null, RECIPE_ID, "key/a.jpg", "LOCAL", "image/jpeg", 200L, true,  0));
            repository.save(new RecipeMedia(null, RECIPE_ID, "key/b.jpg", "LOCAL", "image/png",  300L, false, 1));
            return null;
        });

        List<RecipeMedia> results = repository.findByRecipeId(RECIPE_ID);

        assertEquals(3, results.size(), "Must return the 3 media items for the recipe");
        assertEquals(0, results.get(0).sortOrder(), "First element must have sort_order 0");
        assertEquals(1, results.get(1).sortOrder(), "Second element must have sort_order 1");
        assertEquals(2, results.get(2).sortOrder(), "Third element must have sort_order 2");
    }

    @Test
    @DisplayName("findByRecipeId: Should return an empty list when the recipe has no media")
    void findByRecipeId_ShouldReturnEmptyList_WhenNoMedia() {
        List<RecipeMedia> results = repository.findByRecipeId(RECIPE_ID);

        assertTrue(results.isEmpty(), "The list must be empty when there is no media");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("delete: Should remove the media from the database")
    void delete_ShouldRemoveEntityFromDatabase() {
        RecipeMedia saved = transactionManager.execute(() -> repository.save(buildMedia(false, 0)));
        RecipeMediaId mediaId = saved.id();

        transactionManager.execute(() -> { repository.delete(mediaId); return null; });

        assertTrue(repository.findById(mediaId).isEmpty(), "The entity must have been deleted");
    }

    @Test
    @DisplayName("delete: Should not throw when the ID does not exist")
    void delete_ShouldNotThrow_WhenIdDoesNotExist() {
        assertDoesNotThrow(() ->
                transactionManager.execute(() -> { repository.delete(new RecipeMediaId(9999)); return null; }));
    }
}
