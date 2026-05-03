package com.recetea.infrastructure.persistence.recipe.jdbc.mappers;

import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hydrates a single {@code difficulties} row into a {@link Difficulty}
 * domain record. The column alias {@code difficulty_level} is used here
 * (not {@code name}) because the SQL schema names that column for the
 * label — kept consistent with the alias used by
 * {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE}.
 *
 * <p><b>ES — </b>Hidrata una única fila de {@code difficulties} en un
 * record del dominio {@link Difficulty}. Aquí se usa el alias de
 * columna {@code difficulty_level} (no {@code name}) porque el
 * esquema SQL nombra esa columna para la etiqueta — se mantiene
 * coherente con el alias usado por
 * {@code RecipeSqlRegistry.SELECT_FULL_AGGREGATE}.
 */
public class DifficultyMapper implements RowMapper<Difficulty> {
    @Override
    public Difficulty map(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    public static Difficulty mapRow(ResultSet rs) throws SQLException {
        return new Difficulty(new DifficultyId(rs.getInt("difficulty_id")), rs.getString("difficulty_level"));
    }
}
