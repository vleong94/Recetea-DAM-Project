package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;
import java.sql.*;
import java.util.*;

public abstract class BaseJdbcRepository {
    protected final JdbcTransactionManager transactionManager;

    protected BaseJdbcRepository(JdbcTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    protected <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection conn = transactionManager.getConnection();
             PreparedStatement ps = prepareStatement(conn, sql, params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { results.add(mapper.map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error SQL: " + sql, e); }
        return results;
    }

    protected <T> Optional<T> queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = transactionManager.getConnection();
             PreparedStatement ps = prepareStatement(conn, sql, params);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) { return Optional.of(mapper.map(rs)); }
        } catch (SQLException e) { throw new RuntimeException("Error SQL: " + sql, e); }
        return Optional.empty();
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) { ps.setObject(i + 1, params[i]); }
        return ps;
    }
}