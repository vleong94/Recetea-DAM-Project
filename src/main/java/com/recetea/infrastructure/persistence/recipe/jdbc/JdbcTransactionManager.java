package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.shared.domain.utils.ExecutionContext;
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
 *
 * CID propagation: {@code execute} delegates to {@link ExecutionContext#call} so a
 * correlation ID is always bound (and MDC populated) for the duration of the JDBC work.
 * If a CID is already in scope from an outer wrapper, ExecutionContext reuses it; if the
 * flow starts at this transaction manager, ExecutionContext sources a fresh one.
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
        return ExecutionContext.call(() -> doExecute(action));
    }

    private <T> T doExecute(Supplier<T> action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ScopedValue.where(...).call(...) unbinds CONNECTION on every exit path
                // (normal return, RuntimeException, Error). No manual cleanup needed.
                T result = ScopedValue.where(CONNECTION, conn).call(action::get);
                conn.commit();
                return result;
            } catch (Exception e) {
                // Attempt rollback, but never let a rollback failure mask the original cause.
                // Suppressing instead of replacing preserves the business error in stack traces
                // and exception chaining — what the GlobalExceptionHandler eventually logs.
                try {
                    conn.rollback();
                } catch (SQLException rollbackErr) {
                    e.addSuppressed(rollbackErr);
                }
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
