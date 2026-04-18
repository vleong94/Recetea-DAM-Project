package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoryMapper implements RowMapper<Category> {
    @Override
    public Category map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static Category mapRow(ResultSet rs) throws SQLException {
        return new Category(new CategoryId(rs.getInt("id_category")), rs.getString("name"));
    }
}
