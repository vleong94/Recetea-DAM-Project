package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code recipe_media} row into a {@link RecipeMedia}
 * record. Used by {@link com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcRecipeMediaRepository}
 * for direct row reads — the LATERAL JSONB hydration path
 * ({@code RecipeMapper.mapMediaJson}) bypasses this mapper entirely
 * since it parses from JSON, not a ResultSet.
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code recipe_media} en un
 * record {@link RecipeMedia}. Lo usa
 * {@link com.recetea.infrastructure.persistence.recipe.jdbc.repositories.JdbcRecipeMediaRepository}
 * para lecturas directas de filas — la ruta de hidratación LATERAL
 * JSONB ({@code RecipeMapper.mapMediaJson}) se salta este mapper por
 * completo, ya que parsea desde JSON, no desde un ResultSet.
 */
public class RecipeMediaMapper implements RowMapper<RecipeMedia> {

    @Override
    public RecipeMedia map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static RecipeMedia mapRow(ResultSet rs) throws SQLException {
        return new RecipeMedia(
                new RecipeMediaId(rs.getInt("media_id")),
                new RecipeId(rs.getInt("recipe_id")),
                rs.getString("storage_key"),
                rs.getString("storage_provider"),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                rs.getBoolean("is_main"),
                rs.getInt("sort_order"));
    }
}
