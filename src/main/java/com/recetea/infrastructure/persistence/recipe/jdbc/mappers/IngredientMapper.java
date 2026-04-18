package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IngredientMapper implements RowMapper<Ingredient> {
    @Override
    public Ingredient map(ResultSet rs) throws SQLException {
        return new Ingredient(
                new IngredientId(rs.getInt("id_ingredient")),
                new CategoryId(rs.getInt("ing_category_id")),
                rs.getString("name")
        );
    }
}
