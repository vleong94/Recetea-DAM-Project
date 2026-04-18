package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DifficultyMapper implements RowMapper<Difficulty> {
    @Override
    public Difficulty map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static Difficulty mapRow(ResultSet rs) throws SQLException {
        return new Difficulty(new DifficultyId(rs.getInt("id_difficulty")), rs.getString("level_name"));
    }
}
