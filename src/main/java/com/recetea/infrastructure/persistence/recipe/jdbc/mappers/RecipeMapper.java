package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.PreparationTime;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Servings;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.core.user.domain.UserId;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RecipeMapper {

    private RecipeMapper() {}

    public static Recipe mapRow(ResultSet rs) throws SQLException {
        Recipe recipe = new Recipe(
                new UserId(rs.getInt("user_id")),
                CategoryMapper.mapRow(rs),
                DifficultyMapper.mapRow(rs),
                rs.getString("title"),
                rs.getString("description"),
                new PreparationTime(rs.getInt("prep_time_min")),
                new Servings(rs.getInt("servings"))
        );
        recipe.setId(new RecipeId(rs.getInt("id_recipe")));
        return recipe;
    }

    public static RecipeIngredient mapIngredientRow(ResultSet rs) throws SQLException {
        return new RecipeIngredient(
                new IngredientId(rs.getInt("ingredient_id")),
                new UnitId(rs.getInt("unit_id")),
                rs.getBigDecimal("quantity"),
                rs.getString("ing_name"),
                rs.getString("unit_abbr")
        );
    }

    public static RecipeStep mapStepRow(ResultSet rs) throws SQLException {
        return new RecipeStep(
                rs.getInt("step_order"),
                rs.getString("instruction")
        );
    }
}
