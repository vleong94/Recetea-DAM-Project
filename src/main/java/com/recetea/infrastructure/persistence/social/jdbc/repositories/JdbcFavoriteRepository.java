package com.recetea.infrastructure.persistence.social.jdbc.repositories;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseJdbcRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcFavoriteRepository extends BaseJdbcRepository implements IFavoriteRepository {

    private static final String INSERT =
            "INSERT INTO favorites (user_id, recipe_id) VALUES (?, ?)";
    private static final String DELETE =
            "DELETE FROM favorites WHERE user_id = ? AND recipe_id = ?";
    private static final String EXISTS =
            "SELECT 1 FROM favorites WHERE user_id = ? AND recipe_id = ?";
    private static final String SELECT_BY_USER =
            "SELECT recipe_id FROM favorites WHERE user_id = ?";

    public JdbcFavoriteRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public void save(UserId userId, RecipeId recipeId) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to save favourite.", e);
        } finally {
            closeIfNonTransactional(conn, INSERT);
        }
    }

    @Override
    public void delete(UserId userId, RecipeId recipeId) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new InfrastructureException("Failed to delete favourite.", e);
        } finally {
            closeIfNonTransactional(conn, DELETE);
        }
    }

    @Override
    public boolean isFavorite(UserId userId, RecipeId recipeId) {
        return queryForObject(EXISTS, rs -> true, userId.value(), recipeId.value()).isPresent();
    }

    @Override
    public List<RecipeId> findFavoriteRecipeIdsByUserId(UserId userId) {
        return queryForList(SELECT_BY_USER, rs -> new RecipeId(rs.getInt("recipe_id")), userId.value());
    }
}
