package com.recetea.infrastructure.persistence.recipe.jdbc;

import com.recetea.core.shared.application.ports.out.ITransactionManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Adaptador de infraestructura que implementa la gestión de transacciones JDBC.
 * Utiliza un ThreadLocal para vincular la conexión activa al hilo de ejecución actual,
 * permitiendo que múltiples repositorios compartan la misma sesión transaccional
 * sin necesidad de pasar la conexión como parámetro en los métodos.
 */
public class JdbcTransactionManager implements ITransactionManager {

    private final DataSource dataSource;
    private static final ThreadLocal<Connection> CONNECTION_HOLDER = new ThreadLocal<>();

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Provee acceso a la conexión vinculada al hilo actual.
     * Si no existe una conexión activa, solicita una nueva al pool. Este método es
     * consumido por los Repositories para obtener el canal de comunicación.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn == null || conn.isClosed()) {
            return dataSource.getConnection();
        }
        return conn;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            CONNECTION_HOLDER.set(conn);
            try {
                T result = action.get();
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                CONNECTION_HOLDER.remove();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fallo crítico en el Transaction Bridge de JDBC.", e);
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