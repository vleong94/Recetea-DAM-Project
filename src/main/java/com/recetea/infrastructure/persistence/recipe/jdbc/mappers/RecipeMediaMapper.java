package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RecipeMediaMapper implements RowMapper<RecipeMedia> {

    @Override
    public RecipeMedia map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static RecipeMedia mapRow(ResultSet rs) throws SQLException {
        return new RecipeMedia(
                new RecipeMediaId(rs.getInt("id_media")),
                new RecipeId(rs.getInt("recipe_id")),
                rs.getString("storage_key"),
                rs.getString("storage_provider"),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                rs.getBoolean("is_main"),
                rs.getInt("sort_order"));
    }
}
