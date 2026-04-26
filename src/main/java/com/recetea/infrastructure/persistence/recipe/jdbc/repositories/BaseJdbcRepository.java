package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public abstract class BaseJdbcRepository {

    private static final Logger log = LoggerFactory.getLogger(BaseJdbcRepository.class);
    protected final JdbcTransactionManager transactionManager;

    protected BaseJdbcRepository(JdbcTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    protected <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = prepareStatement(conn, sql, params);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { results.add(mapper.map(rs)); }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("SQL error in queryForList: " + sql, e);
        } finally {
            closeIfNonTransactional(conn, sql);
        }
        return results;
    }

    protected <T> Optional<T> queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = prepareStatement(conn, sql, params);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { return Optional.of(mapper.map(rs)); }
            }
        } catch (SQLException e) {
            throw new InfrastructureException("SQL error in queryForObject: " + sql, e);
        } finally {
            closeIfNonTransactional(conn, sql);
        }
        return Optional.empty();
    }

    protected void closeIfNonTransactional(Connection conn, String context) {
        if (conn != null && !transactionManager.isTransactional()) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.warn("Failed to close non-transactional connection [context={}]: {}", context, e.getMessage());
            }
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) { ps.setObject(i + 1, params[i]); }
        return ps;
    }
}