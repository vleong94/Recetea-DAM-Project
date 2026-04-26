package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.media.IRecipeMediaRepository;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.RecipeMediaMapper;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class JdbcRecipeMediaRepository extends BaseJdbcRepository implements IRecipeMediaRepository {

    private final RecipeMediaMapper mapper = new RecipeMediaMapper();

    private static final String INSERT =
            "INSERT INTO recipe_media (recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID =
            "SELECT id_media, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
            "FROM recipe_media WHERE id_media = ?";
    private static final String SELECT_BY_RECIPE =
            "SELECT id_media, recipe_id, storage_key, storage_provider, mime_type, size_bytes, is_main, sort_order " +
            "FROM recipe_media WHERE recipe_id = ? ORDER BY sort_order ASC";
    private static final String DELETE =
            "DELETE FROM recipe_media WHERE id_media = ?";

    public JdbcRecipeMediaRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public RecipeMedia save(RecipeMedia media) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, media.recipeId().value());
                ps.setString(2, media.storageKey());
                ps.setString(3, media.storageProvider());
                ps.setString(4, media.mimeType());
                ps.setLong(5, media.sizeBytes());
                ps.setBoolean(6, media.isMain());
                ps.setInt(7, media.sortOrder());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new RecipeMedia(
                                new RecipeMediaId(keys.getInt(1)),
                                media.recipeId(),
                                media.storageKey(),
                                media.storageProvider(),
                                media.mimeType(),
                                media.sizeBytes(),
                                media.isMain(),
                                media.sortOrder());
                    }
                }
            }
            throw new InfrastructureException("No generated key returned for recipe_media insert.", null);
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to persist recipe media.", e);
        } finally {
            closeIfNonTransactional(conn, INSERT);
        }
    }

    @Override
    public Optional<RecipeMedia> findById(RecipeMediaId id) {
        return queryForObject(SELECT_BY_ID, mapper, id.value());
    }

    @Override
    public List<RecipeMedia> findByRecipeId(RecipeId recipeId) {
        return queryForList(SELECT_BY_RECIPE, mapper, recipeId.value());
    }

    @Override
    public void delete(RecipeMediaId id) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setInt(1, id.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to delete media item ID: " + id.value(), e);
        } finally {
            closeIfNonTransactional(conn, DELETE);
        }
    }

}
