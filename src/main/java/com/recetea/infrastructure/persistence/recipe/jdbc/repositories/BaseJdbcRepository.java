package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.InfrastructureException;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.RowMapper;

import java.sql.*;
import java.util.*;

/**
 * Common foundation for every JDBC repository in the project. Owns the two
 * pieces of plumbing every adapter needs: connection acquisition that
 * cooperates with the {@link JdbcTransactionManager}, and a metrics-port
 * hook that records query timings without each subclass having to wire it.
 *
 * <p><b>Connection lifecycle.</b>
 * {@link #withConnection(ConnectionAction, String)} routes on whether a
 * transaction is currently active:
 * <ul>
 *   <li>If a {@code ScopedValue} CONNECTION is bound, the existing
 *       transaction-owned connection is reused and NOT closed — closing
 *       belongs to the transaction's owner.</li>
 *   <li>Otherwise a fresh connection is acquired from the data source and
 *       closed deterministically via try-with-resources, eliminating
 *       leak risk.</li>
 * </ul>
 *
 * <p><b>Error mapping.</b> {@link SQLException} is wrapped into
 * {@link InfrastructureException} (technical code {@code INF-500}) with a
 * context label embedded in the message — the SQL itself doubles as the
 * label so log lines tie failures to the originating statement.
 *
 * <p><b>Metrics.</b> Every {@code withConnection} invocation records
 * elapsed nanos to {@link IMetricsPort#recordQueryTime}. The
 * {@code LogMetricsAdapter} promotes anything above 500 ms to WARN; the
 * NoOp adapter is a silent default for tests.
 *
 * <p><b>ES — </b>Cimiento común para cada repositorio JDBC del
 * proyecto. Es dueño de las dos piezas de plumbing que cada adaptador
 * necesita: la adquisición de conexión que coopera con
 * {@link JdbcTransactionManager}, y un hook al puerto de métricas que
 * registra los tiempos de consulta sin que cada subclase tenga que
 * cablearlo.
 *
 * <p><b>Ciclo de vida de la conexión.</b>
 * {@link #withConnection(ConnectionAction, String)} enruta según si
 * hay una transacción activa:
 * <ul>
 *   <li>Si la CONNECTION del {@code ScopedValue} está ligada, se
 *       reutiliza la conexión existente propiedad de la transacción
 *       y NO se cierra — el cierre es responsabilidad del propietario
 *       de la transacción.</li>
 *   <li>En caso contrario se adquiere una conexión fresca del data
 *       source y se cierra de forma determinista vía
 *       try-with-resources, eliminando el riesgo de fuga.</li>
 * </ul>
 *
 * <p><b>Mapeo de errores.</b> {@link SQLException} se envuelve en
 * {@link InfrastructureException} (código técnico {@code INF-500})
 * con una etiqueta de contexto incrustada en el mensaje — la propia
 * SQL hace de etiqueta para que las líneas de log liguen los fallos
 * con la sentencia originaria.
 *
 * <p><b>Métricas.</b> Cada invocación de {@code withConnection}
 * registra los nanosegundos transcurridos en
 * {@link IMetricsPort#recordQueryTime}. {@code LogMetricsAdapter}
 * eleva todo lo que esté por encima de 500 ms a WARN; el adaptador
 * NoOp es el silencioso por defecto para tests.
 */
public abstract class BaseJdbcRepository {

    protected final JdbcTransactionManager transactionManager;

    /**
     * Per-instance metrics port — explicit dependency injection. Threaded through
     * every subclass constructor and forwarded via {@code super(transactionManager,
     * metricsPort)}, so the composition root has the only authoritative wiring.
     */
    protected final IMetricsPort metricsPort;

    protected BaseJdbcRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null.");
        this.metricsPort        = Objects.requireNonNull(metricsPort,        "metricsPort must not be null.");
    }

    @FunctionalInterface
    protected interface ConnectionAction<T> {
        T apply(Connection conn) throws SQLException;
    }

    /**
     * Executes {@code action} on an appropriate {@link Connection}.
     *
     * If a transaction is already active on the current thread (ScopedValue is bound),
     * the existing connection is reused and NOT closed — closing is the transaction's
     * responsibility. Otherwise a fresh connection is obtained from the DataSource and
     * closed deterministically via try-with-resources, eliminating any leak risk.
     */
    protected <T> T withConnection(ConnectionAction<T> action, String context) {
        long start = System.nanoTime();
        try {
            if (transactionManager.isTransactional()) {
                try {
                    return action.apply(JdbcTransactionManager.CONNECTION.get());
                } catch (RuntimeException e) {
                    throw e;
                } catch (SQLException e) {
                    throw new InfrastructureException("SQL error [" + context + "]", e);
                }
            }
            try (Connection conn = transactionManager.getConnection()) {
                return action.apply(conn);
            } catch (RuntimeException e) {
                throw e;
            } catch (SQLException e) {
                throw new InfrastructureException("SQL error [" + context + "]", e);
            }
        } finally {
            this.metricsPort.recordQueryTime(context, System.nanoTime() - start);
        }
    }

    protected <T> List<T> queryForList(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(conn -> {
            List<T> results = new ArrayList<>();
            try (PreparedStatement ps = prepareStatement(conn, sql, params);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapper.map(rs));
            }
            return results;
        }, sql);
    }

    protected <T> Optional<T> queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(conn -> {
            try (PreparedStatement ps = prepareStatement(conn, sql, params);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapper.map(rs));
            }
            return Optional.empty();
        }, sql);
    }

    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps;
    }
}
