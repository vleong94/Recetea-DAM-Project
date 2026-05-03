package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code ingredients} row into an {@link Ingredient}
 * record. {@code ingredient_category_id} maps to {@code Ingredient.categoryId}
 * — the column is named after its semantic role (ingredient grouping)
 * rather than its FK target (the {@code categories} table is shared with
 * recipes).
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code ingredients} en un
 * record {@link Ingredient}. {@code ingredient_category_id} mapea a
 * {@code Ingredient.categoryId} — la columna se nombra según su rol
 * semántico (agrupación de ingredientes) en lugar de su objetivo FK
 * (la tabla {@code categories} se comparte con las recetas).
 */
public class IngredientMapper implements RowMapper<Ingredient> {
    @Override
    public Ingredient map(ResultSet rs) throws SQLException {
        return new Ingredient(
                new IngredientId(rs.getInt("ingredient_id")),
                new CategoryId(rs.getInt("ingredient_category_id")),
                rs.getString("name")
        );
    }
}
