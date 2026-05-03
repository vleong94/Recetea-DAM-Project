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

/**
 * Hydrates the recipe aggregate from JDBC + JSONB. Pairs with
 * {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE} — every column alias
 * and JSONB key here is part of the contract with that SQL.
 *
 * <p><b>Why JSONB instead of result-set walking.</b> The detail view
 * needs four child collections (ingredients, steps, ratings, media) +
 * the parent row in one round-trip. PostgreSQL's
 * {@code JSONB_AGG(JSONB_BUILD_OBJECT(...))} per LATERAL block emits
 * each collection as a single column the JDBC driver returns as text;
 * {@code org.json} parses each into a {@code JSONArray} that the four
 * {@code mapXxxJson} methods walk into domain records.
 *
 * <p>{@code mapRow(...)} requires every child collection up front —
 * {@code Recipe} is an immutable record with a 14-arg canonical
 * constructor, so partial hydration isn't possible. Caller responsibility:
 * read the four JSON columns, parse each, then call {@code mapRow} once.
 *
 * <p><b>ES — </b>Hidrata el agregado de receta desde JDBC + JSONB. Va
 * de la mano con {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE} —
 * cada alias de columna y clave JSONB aquí son parte del contrato
 * con esa SQL.
 *
 * <p><b>Por qué JSONB en lugar de recorrer el ResultSet.</b> La vista
 * de detalle necesita cuatro colecciones hijas (ingredientes, pasos,
 * valoraciones, multimedia) + la fila padre en un único round-trip.
 * El {@code JSONB_AGG(JSONB_BUILD_OBJECT(...))} de PostgreSQL por
 * cada bloque LATERAL emite cada colección como una única columna
 * que el driver JDBC devuelve como texto; {@code org.json} parsea
 * cada una en un {@code JSONArray} que los cuatro métodos
 * {@code mapXxxJson} recorren para construir los records del
 * dominio.
 *
 * <p>{@code mapRow(...)} requiere cada colección hija de antemano —
 * {@code Recipe} es un record inmutable con un constructor canónico
 * de 14 argumentos, así que no es posible una hidratación parcial.
 * Responsabilidad del llamador: leer las cuatro columnas JSON,
 * parsear cada una y llamar una sola vez a {@code mapRow}.
 */
public class RecipeMapper {

    private RecipeMapper() {}

    /**
     * Builds a fully hydrated {@link Recipe} record from one row + the four pre-mapped
     * child collections. The single call-site in {@code JdbcRecipeRepository.findById}
     * reads the LATERAL-aggregated JSON columns first, then hands them to this method
     * so the record's compact constructor sees every component at once (immutable: no
     * post-construction hydration is possible).
     */
    public static Recipe mapRow(ResultSet rs,
                                List<RecipeIngredient> ingredients,
                                List<RecipeStep> steps,
                                List<Rating> ratings,
                                List<RecipeMedia> mediaItems) throws SQLException {
        BigDecimal avgScore = rs.getBigDecimal("average_score");
        return new Recipe(
                new RecipeId(rs.getInt("recipe_id")),
                new UserId(rs.getInt("user_id")),
                new Category(new CategoryId(rs.getInt("category_id")), rs.getString("category_name")),
                new Difficulty(new DifficultyId(rs.getInt("difficulty_id")), rs.getString("difficulty_level")),
                rs.getString("title"),
                rs.getString("description"),
                new PreparationTime(rs.getInt("prep_time_min")),
                new Servings(rs.getInt("servings")),
                avgScore != null ? avgScore : BigDecimal.ZERO,
                rs.getInt("total_ratings"),
                ingredients, steps, ratings, mediaItems);
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
                    new RecipeMediaId(obj.getInt("media_id")),
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
