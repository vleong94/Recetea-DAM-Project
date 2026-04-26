package com.recetea.infrastructure.persistence.user.jdbc.repositories;

import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseJdbcRepository;
import com.recetea.infrastructure.persistence.user.jdbc.mappers.UserMapper;

import java.sql.*;
import java.util.Optional;

public class JdbcUserRepository extends BaseJdbcRepository implements IUserRepository {

    private final UserMapper mapper = new UserMapper();

    private static final String INSERT_USER =
            "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
    private static final String SELECT_BY_ID =
            "SELECT id_user, username, email, password_hash FROM users WHERE id_user = ?";
    private static final String SELECT_BY_USERNAME =
            "SELECT id_user, username, email, password_hash FROM users WHERE username = ?";
    private static final String SELECT_BY_EMAIL =
            "SELECT id_user, username, email, password_hash FROM users WHERE email = ?";

    public JdbcUserRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public void save(User user) {
        Connection conn = null;
        try {
            conn = transactionManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPasswordHash());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) user.setId(new UserId(rs.getInt(1)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist user.", e);
        } finally {
            closeIfNonTransactional(conn, INSERT_USER);
        }
    }

    @Override
    public Optional<User> findById(UserId id) {
        return queryForObject(SELECT_BY_ID, mapper, id.value());
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return queryForObject(SELECT_BY_USERNAME, mapper, username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryForObject(SELECT_BY_EMAIL, mapper, email);
    }
}
