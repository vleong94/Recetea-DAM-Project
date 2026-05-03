package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smart-sync gateway for the {@code steps} table. Mirror of
 * {@link RecipeIngredientTableGateway}'s diff strategy keyed on
 * {@code step_order} (the natural unique within a recipe).
 *
 * <p>Three batched passes per sync: DELETE for orders dropped,
 * INSERT for new orders, UPDATE for instructions that changed text.
 * The {@code stepOrder} is the join key — re-ordering an existing step
 * therefore reads as a DELETE + INSERT pair (the old order goes away,
 * the new order arrives), not as an UPDATE. This is fine because the
 * UI's renumbering already mutates the {@code stepOrder} component.
 *
 * <p>Package-private — {@link JdbcRecipeRepository} is the only caller.
 *
 * <p><b>ES — </b>Gateway de smart-sync para la tabla {@code steps}.
 * Espejo de la estrategia de diff de
 * {@link RecipeIngredientTableGateway}, indexada por
 * {@code step_order} (la unicidad natural dentro de una receta).
 *
 * <p>Tres pasadas en batch por cada sync: DELETE para los órdenes
 * que desaparecen, INSERT para órdenes nuevos, UPDATE para
 * instrucciones cuyo texto cambió. {@code stepOrder} es la clave de
 * join — reordenar un paso existente se lee por tanto como un par
 * DELETE + INSERT (el orden viejo desaparece, el orden nuevo
 * llega), no como un UPDATE. Esto es correcto porque la
 * renumeración de la UI ya muta el componente {@code stepOrder}.
 *
 * <p>Package-private — {@link JdbcRecipeRepository} es el único
 * llamador.
 */
class RecipeStepTableGateway extends BaseJdbcRepository {

    private static final String SELECT_STEPS =
            "SELECT step_order, instruction FROM steps WHERE recipe_id = ?";
    private static final String INSERT_STEP =
            "INSERT INTO steps (recipe_id, step_order, instruction) VALUES (?, ?, ?)";
    private static final String UPDATE_STEP =
            "UPDATE steps SET instruction = ? WHERE recipe_id = ? AND step_order = ?";
    private static final String DELETE_STEP =
            "DELETE FROM steps WHERE recipe_id = ? AND step_order = ?";

    RecipeStepTableGateway(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    void insertSteps(Recipe recipe, RecipeId id) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_STEP)) {
                for (RecipeStep step : recipe.getSteps()) {
                    ps.setInt(1, id.value());
                    ps.setInt(2, step.stepOrder());
                    ps.setString(3, step.instruction());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        }, "insert steps for recipe id=" + id.value());
    }

    void syncSteps(Recipe recipe) {
        withConnection(conn -> {
            doSync(conn, recipe);
            return null;
        }, "sync steps for recipe id=" + recipe.getId().value());
    }

    private void doSync(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, String> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_STEPS)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.put(rs.getInt("step_order"), rs.getString("instruction"));
                }
            }
        }

        Map<Integer, RecipeStep> incoming = new LinkedHashMap<>();
        for (RecipeStep s : recipe.getSteps()) {
            incoming.put(s.stepOrder(), s);
        }

        try (PreparedStatement ps = conn.prepareStatement(DELETE_STEP)) {
            for (int order : existing.keySet()) {
                if (!incoming.containsKey(order)) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, order);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_STEP)) {
            for (RecipeStep s : recipe.getSteps()) {
                if (!existing.containsKey(s.stepOrder())) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, s.stepOrder());
                    ps.setString(3, s.instruction());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STEP)) {
            for (RecipeStep s : recipe.getSteps()) {
                String existingInstruction = existing.get(s.stepOrder());
                if (existingInstruction != null && !existingInstruction.equals(s.instruction())) {
                    ps.setString(1, s.instruction());
                    ps.setInt(2, recipeId);
                    ps.setInt(3, s.stepOrder());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
