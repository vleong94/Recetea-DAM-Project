package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UnitMapper implements RowMapper<Unit> {
    @Override
    public Unit map(ResultSet rs) throws SQLException {
        return new Unit(
                new UnitId(rs.getInt("id_unit")),
                rs.getString("name"),
                rs.getString("abbreviation")
        );
    }
}
