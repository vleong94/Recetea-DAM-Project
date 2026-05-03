package com.recetea.infrastructure.persistence.user.jdbc.repositories;

import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.User;
import com.recetea.core.user.domain.UserId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.repositories.BaseJdbcRepository;
import com.recetea.infrastructure.persistence.user.jdbc.mappers.UserMapper;

import java.sql.*;
import java.util.Optional;

/**
 * JDBC adapter for the {@code users} table. Three lookup variants —
 * by id, by username, by email — feed the login flow's two-probe path
 * (try username first, fall back to email).
 *
 * <p>No caching: users are mutable (registration adds rows, the login
 * path is hot for new sessions) and the working set is small enough
 * that the per-call round-trip is cheap. The connection-pool warm path
 * keeps lookup latency under 5 ms steady-state.
 *
 * <p><b>ES — </b>Adaptador JDBC para la tabla {@code users}. Tres
 * variantes de búsqueda — por id, por username, por email — alimentan
 * la ruta de doble sondeo del flujo de login (intenta primero por
 * username y cae a email).
 *
 * <p>Sin caché: los usuarios son mutables (el registro añade filas,
 * la ruta de login es activa para sesiones nuevas) y el conjunto de
 * trabajo es lo bastante pequeño para que el round-trip por llamada
 * sea barato. La ruta caliente del pool de conexiones mantiene la
 * latencia de búsqueda bajo 5 ms en régimen estable.
 */
public class JdbcUserRepository extends BaseJdbcRepository implements IUserRepository {

    private final UserMapper mapper = new UserMapper();

    private static final String INSERT_USER =
            "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
    private static final String SELECT_BY_ID =
            "SELECT user_id, username, email, password_hash FROM users WHERE user_id = ?";
    private static final String SELECT_BY_USERNAME =
            "SELECT user_id, username, email, password_hash FROM users WHERE username = ?";
    private static final String SELECT_BY_EMAIL =
            "SELECT user_id, username, email, password_hash FROM users WHERE email = ?";

    public JdbcUserRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public UserId save(User user) {
        return withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.username().value());
                ps.setString(2, user.email().value());
                ps.setString(3, user.passwordHash().value());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return new UserId(rs.getInt(1));
                }
            }
            throw new RuntimeException("No generated key returned after user insert.");
        }, INSERT_USER);
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
