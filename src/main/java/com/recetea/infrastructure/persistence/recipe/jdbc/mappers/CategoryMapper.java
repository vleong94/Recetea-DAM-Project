package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code categories} row into a {@link Category} domain record.
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code categories} en un
 * record del dominio {@link Category}.
 */
public class CategoryMapper implements RowMapper<Category> {
    @Override
    public Category map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static Category mapRow(ResultSet rs) throws SQLException {
        return new Category(new CategoryId(rs.getInt("category_id")), rs.getString("name"));
    }
}
