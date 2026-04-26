package com.recetea.infrastructure.persistence.recipe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Functional interface for transforming a single {@link ResultSet} row into a domain object.
 * Decouples row-extraction logic from the query-control flow in {@code BaseJdbcRepository}.
 *
 * @param <T> The target object type.
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the given {@link ResultSet} to an instance of {@code T}.
     *
     * @param rs ResultSet positioned at the row to process.
     * @return The mapped object.
     * @throws SQLException if a column cannot be read.
     */
    T map(ResultSet rs) throws SQLException;
}
