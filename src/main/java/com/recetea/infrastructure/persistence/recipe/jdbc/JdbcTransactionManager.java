package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Virtual-thread compatible: {@code ScopedValue} binds the {@link Connection} to the
 * <em>virtual thread</em> that calls {@link #execute}, not to its carrier platform thread.
 * The binding survives park/unmount cycles transparently, so JDBC calls that block (e.g.
 * waiting for network I/O) correctly release the carrier without corrupting the scope.
 *
 * The nested-transaction guard ({@code CONNECTION.isBound()}) remains correct under
 * virtual threads: each virtual thread has its own scope, so two concurrent transactions
 * on different virtual threads do not interfere.
 */
public class JdbcTransactionManager implements ITransactionManager {

    public static final ScopedValue<Connection> CONNECTION = ScopedValue.newInstance();

    private final DataSource dataSource;

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isTransactional() {
        return CONNECTION.isBound();
    }

    public Connection getConnection() throws SQLException {
        if (CONNECTION.isBound()) {
            return CONNECTION.get();
        }
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(true);
        return conn;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        if (CONNECTION.isBound()) {
            throw new IllegalStateException("Nested transaction not allowed: a transaction is already active on this thread.");
        }
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = ScopedValue.where(CONNECTION, conn).call(action::get);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new InfrastructureException("Unrecoverable error during transactional execution.", e);
            }
        } catch (InfrastructureException | IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new InfrastructureException("Critical failure in the JDBC transaction bridge.", e);
        }
    }

    @Override
    public void execute(Runnable action) {
        execute(() -> {
            action.run();
            return null;
        });
    }
}
