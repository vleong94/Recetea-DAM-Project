package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

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
            throw new IllegalStateException("Transacción anidada no permitida: ya existe una transacción activa en este hilo.");
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
                throw new InfrastructureException("Error no recuperable durante la ejecución transaccional.", e);
            }
        } catch (InfrastructureException | IllegalStateException e) {
            throw e;
        } catch (SQLException e) {
            throw new InfrastructureException("Fallo crítico en el Transaction Bridge de JDBC.", e);
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
