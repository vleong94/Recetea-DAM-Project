package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Smart-sync gateway for the {@code recipe_ingredients} junction table.
 * Owns the four DML statements local to ingredient diffing — none of
 * them are reused outside this class, so they live here rather than in
 * {@link RecipeSqlRegistry} which tracks aggregate-wide queries.
 *
 * <p><b>Diff strategy.</b> {@link #syncIngredients(Recipe)} reads the
 * existing rows, pairs them with the incoming aggregate by
 * {@code ingredient_id}, then runs three batched passes: DELETE rows the
 * aggregate dropped, INSERT new rows, UPDATE rows whose unit or quantity
 * changed. The order matters — DELETE runs first to release any unique
 * constraint that a subsequent INSERT might collide with on a
 * partial-overlap edit.
 *
 * <p>Quantity is stored at scale 2 ({@code RoundingMode.HALF_UP}) to
 * match the {@code numeric(10,2)} column; the comparison
 * {@code BigDecimal.compareTo} ignores trailing-zero scale differences,
 * so an in-memory {@code 1.5} and a stored {@code 1.50} read as equal
 * (no false-positive UPDATE).
 *
 * <p>Package-private — {@link JdbcRecipeRepository} is the only caller.
 *
 * <p><b>ES — </b>Gateway de smart-sync para la tabla de unión
 * {@code recipe_ingredients}. Es dueño de las cuatro sentencias DML
 * locales del diff de ingredientes — ninguna se reutiliza fuera de
 * esta clase, así que viven aquí en lugar de en
 * {@link RecipeSqlRegistry}, que rastrea consultas a nivel de
 * agregado.
 *
 * <p><b>Estrategia de diff.</b> {@link #syncIngredients(Recipe)} lee
 * las filas existentes, las empareja con el agregado de entrada por
 * {@code ingredient_id} y luego ejecuta tres pasadas en batch:
 * DELETE de las filas que el agregado dejó caer, INSERT de las
 * filas nuevas, UPDATE de las filas cuya unidad o cantidad cambió.
 * El orden importa — el DELETE va primero para liberar cualquier
 * restricción única con la que un INSERT posterior pudiera
 * colisionar en una edición de superposición parcial.
 *
 * <p>La cantidad se guarda con escala 2 ({@code RoundingMode.HALF_UP})
 * para coincidir con la columna {@code numeric(10,2)}; la
 * comparación {@code BigDecimal.compareTo} ignora diferencias de
 * escala por ceros a la derecha, así que un {@code 1.5} en memoria
 * y un {@code 1.50} almacenado se leen como iguales (sin UPDATE de
 * falso positivo).
 *
 * <p>Package-private — {@link JdbcRecipeRepository} es el único
 * llamador.
 */
class RecipeIngredientTableGateway extends BaseJdbcRepository {

    private static final String SELECT_INGREDIENTS =
            "SELECT ingredient_id, unit_id, quantity FROM recipe_ingredients WHERE recipe_id = ?";
    private static final String INSERT_INGREDIENT =
            "INSERT INTO recipe_ingredients (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_INGREDIENT =
            "UPDATE recipe_ingredients SET unit_id = ?, quantity = ? WHERE recipe_id = ? AND ingredient_id = ?";
    private static final String DELETE_INGREDIENT =
            "DELETE FROM recipe_ingredients WHERE recipe_id = ? AND ingredient_id = ?";

    RecipeIngredientTableGateway(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    void insertIngredients(Recipe recipe, RecipeId id) {
        withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
                for (RecipeIngredient ing : recipe.getIngredients()) {
                    ps.setInt(1, id.value());
                    ps.setInt(2, ing.ingredientId().value());
                    ps.setInt(3, ing.unitId().value());
                    ps.setBigDecimal(4, ing.quantity().setScale(2, RoundingMode.HALF_UP));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        }, "insert ingredients for recipe id=" + id.value());
    }

    void syncIngredients(Recipe recipe) {
        withConnection(conn -> {
            doSync(conn, recipe);
            return null;
        }, "sync ingredients for recipe id=" + recipe.getId().value());
    }

    private void doSync(Connection conn, Recipe recipe) throws SQLException {
        int recipeId = recipe.getId().value();

        Map<Integer, DbIngredientRow> existing = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_INGREDIENTS)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ingId = rs.getInt("ingredient_id");
                    existing.put(ingId, new DbIngredientRow(ingId, rs.getInt("unit_id"), rs.getBigDecimal("quantity")));
                }
            }
        }

        Map<Integer, RecipeIngredient> incoming = new LinkedHashMap<>();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            incoming.put(ri.ingredientId().value(), ri);
        }

        try (PreparedStatement ps = conn.prepareStatement(DELETE_INGREDIENT)) {
            for (int ingId : existing.keySet()) {
                if (!incoming.containsKey(ingId)) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, ingId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                if (!existing.containsKey(ri.ingredientId().value())) {
                    ps.setInt(1, recipeId);
                    ps.setInt(2, ri.ingredientId().value());
                    ps.setInt(3, ri.unitId().value());
                    ps.setBigDecimal(4, ri.quantity().setScale(2, RoundingMode.HALF_UP));
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_INGREDIENT)) {
            for (RecipeIngredient ri : recipe.getIngredients()) {
                DbIngredientRow row = existing.get(ri.ingredientId().value());
                if (row != null && row.isDifferentFrom(ri)) {
                    ps.setInt(1, ri.unitId().value());
                    ps.setBigDecimal(2, ri.quantity().setScale(2, RoundingMode.HALF_UP));
                    ps.setInt(3, recipeId);
                    ps.setInt(4, ri.ingredientId().value());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private record DbIngredientRow(int ingredientId, int unitId, BigDecimal quantity) {
        boolean isDifferentFrom(RecipeIngredient ri) {
            return unitId != ri.unitId().value()
                    || quantity.compareTo(ri.quantity()) != 0;
        }
    }
}
