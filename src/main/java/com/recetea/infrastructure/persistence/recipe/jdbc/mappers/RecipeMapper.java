package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.*;
import com.recetea.core.user.domain.UserId;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecipeMapper {

    private RecipeMapper() {}

    public static Recipe mapRow(ResultSet rs) throws SQLException {
        Category category = new Category(
                new CategoryId(rs.getInt("category_id")),
                rs.getString("category_name"));
        Difficulty difficulty = new Difficulty(
                new DifficultyId(rs.getInt("difficulty_id")),
                rs.getString("difficulty_level"));
        Recipe recipe = new Recipe(
                new UserId(rs.getInt("user_id")),
                category,
                difficulty,
                rs.getString("title"),
                rs.getString("description"),
                new PreparationTime(rs.getInt("prep_time_min")),
                new Servings(rs.getInt("servings"))
        );
        recipe.setId(new RecipeId(rs.getInt("id_recipe")));
        BigDecimal avgScore = rs.getBigDecimal("average_score");
        recipe.setSocialMetrics(
                avgScore != null ? avgScore : BigDecimal.ZERO,
                rs.getInt("total_ratings"));
        return recipe;
    }

    public static List<RecipeIngredient> mapIngredientsJson(String json) {
        JSONArray arr = new JSONArray(json);
        if (arr.isEmpty()) return List.of();
        List<RecipeIngredient> result = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            result.add(new RecipeIngredient(
                    new IngredientId(obj.getInt("ingredient_id")),
                    new UnitId(obj.getInt("unit_id")),
                    obj.getBigDecimal("quantity"),
                    obj.getString("ing_name"),
                    obj.getString("unit_abbr")));
        }
        return result;
    }

    public static List<RecipeStep> mapStepsJson(String json) {
        JSONArray arr = new JSONArray(json);
        if (arr.isEmpty()) return List.of();
        List<RecipeStep> result = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            result.add(new RecipeStep(
                    obj.getInt("step_order"),
                    obj.getString("instruction")));
        }
        return result;
    }

    public static List<Rating> mapRatingsJson(String json) {
        JSONArray arr = new JSONArray(json);
        if (arr.isEmpty()) return List.of();
        List<Rating> result = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String comment  = obj.isNull("comment")  ? null : obj.getString("comment");
            String username = obj.isNull("username") ? null : obj.getString("username");
            result.add(new Rating(
                    new UserId(obj.getInt("user_id")),
                    new Score(obj.getInt("score")),
                    comment,
                    LocalDateTime.parse(obj.getString("created_at")),
                    username));
        }
        return result;
    }

    public static List<RecipeMedia> mapMediaJson(String json, RecipeId recipeId) {
        JSONArray arr = new JSONArray(json);
        if (arr.isEmpty()) return List.of();
        List<RecipeMedia> result = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            result.add(new RecipeMedia(
                    new RecipeMediaId(obj.getInt("id_media")),
                    recipeId,
                    obj.getString("storage_key"),
                    obj.getString("storage_provider"),
                    obj.getString("mime_type"),
                    obj.getLong("size_bytes"),
                    obj.getBoolean("is_main"),
                    obj.getInt("sort_order")));
        }
        return result;
    }
}
