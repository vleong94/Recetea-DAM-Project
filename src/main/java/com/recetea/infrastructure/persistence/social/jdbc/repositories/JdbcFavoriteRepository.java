package com.recetea.infrastructure.persistence.social.jdbc.repositories;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseJdbcRepository;

import java.sql.*;
import java.util.List;

/**
 * JDBC adapter for the {@code favorites} association table.
 *
 * <p>{@link #isFavorite(UserId, RecipeId)} uses {@code SELECT 1} +
 * {@code Optional.isPresent()} as the lightest possible existence
 * probe — no row-mapping cost, no count aggregation, the planner
 * short-circuits as soon as one row matches.
 *
 * <p>{@link #deleteAllByRecipeId(RecipeId)} is the cross-module
 * cleanup hook called by {@code DeleteRecipeUseCase} before the recipe
 * row itself is removed. The schema's {@code ON DELETE CASCADE} would
 * cover the same case at the row level, but routing through the
 * application layer keeps the use case's intent self-documenting.
 *
 * <p><b>ES — </b>Adaptador JDBC para la tabla de asociación
 * {@code favorites}.
 *
 * <p>{@link #isFavorite(UserId, RecipeId)} usa {@code SELECT 1} +
 * {@code Optional.isPresent()} como el sondeo de existencia más
 * ligero posible — sin coste de mapeo de filas, sin agregación de
 * count, el planner corto-circuita en cuanto coincide una fila.
 *
 * <p>{@link #deleteAllByRecipeId(RecipeId)} es el hook de limpieza
 * entre módulos que llama {@code DeleteRecipeUseCase} antes de
 * eliminar la propia fila de receta. El {@code ON DELETE CASCADE}
 * del esquema cubriría el mismo caso a nivel de fila, pero
 * enrutarlo por la capa de aplicación mantiene la intención del
 * caso de uso autoexplicativa.
 */
public class JdbcFavoriteRepository extends BaseJdbcRepository implements IFavoriteRepository {

    private static final String INSERT =
            "INSERT INTO favorites (user_id, recipe_id) VALUES (?, ?)";
    private static final String DELETE =
            "DELETE FROM favorites WHERE user_id = ? AND recipe_id = ?";
    private static final String DELETE_ALL_BY_RECIPE =
            "DELETE FROM favorites WHERE recipe_id = ?";
    private static final String EXISTS =
            "SELECT 1 FROM favorites WHERE user_id = ? AND recipe_id = ?";
    private static final String SELECT_BY_USER =
            "SELECT recipe_id FROM favorites WHERE user_id = ?";

    public JdbcFavoriteRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public void save(UserId userId, RecipeId recipeId) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                ps.executeUpdate();
            }
            return null;
        }, INSERT);
    }

    @Override
    public void delete(UserId userId, RecipeId recipeId) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setInt(1, userId.value());
                ps.setInt(2, recipeId.value());
                ps.executeUpdate();
            }
            return null;
        }, DELETE);
    }

    @Override
    public void deleteAllByRecipeId(RecipeId recipeId) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(DELETE_ALL_BY_RECIPE)) {
                ps.setInt(1, recipeId.value());
                ps.executeUpdate();
            }
            return null;
        }, DELETE_ALL_BY_RECIPE);
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
