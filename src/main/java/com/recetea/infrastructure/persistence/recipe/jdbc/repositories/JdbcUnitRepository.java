package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.UnitMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JDBC adapter for the unit catalogue. Caches via the same CAS-bound
 * {@link AtomicReference} pattern as the other catalogue adapters.
 * Reads {@code abbreviation} alongside {@code name} so the {@link Unit}
 * record is fully populated for the recipe form's unit combo.
 *
 * <p><b>ES — </b>Adaptador JDBC para el catálogo de unidades. Cachea
 * con el mismo patrón {@link AtomicReference} ligado por CAS que los
 * otros adaptadores de catálogo. Lee {@code abbreviation} junto con
 * {@code name} para que el record {@link Unit} esté totalmente
 * poblado para el combo de unidad del formulario de receta.
 */
public class JdbcUnitRepository extends BaseJdbcRepository implements IUnitRepository {

    private static final String SELECT_ALL = "SELECT unit_id, name, abbreviation FROM unit_measures ORDER BY name ASC";
    private final UnitMapper mapper = new UnitMapper();
    private final AtomicReference<List<Unit>> cache = new AtomicReference<>();

    public JdbcUnitRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public List<Unit> findAll() {
        List<Unit> cached = cache.get();
        if (cached != null) return cached;
        List<Unit> loaded = queryForList(SELECT_ALL, mapper);
        cache.compareAndSet(null, List.copyOf(loaded));
        return cache.get();
    }
}
