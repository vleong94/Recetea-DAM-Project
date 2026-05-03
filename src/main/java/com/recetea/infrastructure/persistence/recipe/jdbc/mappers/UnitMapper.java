package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.recipe.domain.vo.UnitId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code unit_measures} row into a {@link Unit} record.
 * Reads both {@code name} and {@code abbreviation} since the {@link Unit}
 * record exposes both for distinct UI surfaces (combo display vs.
 * compact-table rendering).
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code unit_measures} en un
 * record {@link Unit}. Lee tanto {@code name} como
 * {@code abbreviation} ya que el record {@link Unit} expone ambos
 * para superficies de UI distintas (presentación en combo vs.
 * renderizado en tabla compacta).
 */
public class UnitMapper implements RowMapper<Unit> {
    @Override
    public Unit map(ResultSet rs) throws SQLException {
        return new Unit(
                new UnitId(rs.getInt("unit_id")),
                rs.getString("name"),
                rs.getString("abbreviation")
        );
    }
}
