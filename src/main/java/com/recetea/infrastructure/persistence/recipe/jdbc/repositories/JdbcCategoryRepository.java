package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.CategoryMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JDBC adapter for the category catalogue. Caches the full row set in an
 * {@link AtomicReference} on first {@link #findAll()} via {@code compareAndSet}
 * — the catalogue is treated as immutable at runtime, so subsequent calls
 * skip the round-trip entirely. {@code findById} is implemented as an
 * in-memory linear scan over the cached list, fast enough for a 40-row
 * table that fits in L1.
 *
 * <p><b>ES — </b>Adaptador JDBC para el catálogo de categorías.
 * Cachea el conjunto completo de filas en un {@link AtomicReference}
 * en la primera llamada a {@link #findAll()} vía
 * {@code compareAndSet} — el catálogo se trata como inmutable en
 * runtime, así que las llamadas posteriores se saltan el round-trip
 * por completo. {@code findById} se implementa como un escaneo
 * lineal en memoria sobre la lista cacheada, lo bastante rápido
 * para una tabla de 40 filas que cabe en L1.
 */
public class JdbcCategoryRepository extends BaseJdbcRepository implements ICategoryRepository {

    private static final String SELECT_ALL = "SELECT category_id, name FROM categories ORDER BY name ASC";
    private final CategoryMapper mapper = new CategoryMapper();
    private final AtomicReference<List<Category>> cache = new AtomicReference<>();

    public JdbcCategoryRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public List<Category> findAll() {
        List<Category> cached = cache.get();
        if (cached != null) return cached;
        List<Category> loaded = queryForList(SELECT_ALL, mapper);
        cache.compareAndSet(null, List.copyOf(loaded));
        return cache.get();
    }

    @Override
    public Optional<Category> findById(CategoryId id) {
        return findAll().stream()
                .filter(c -> c.id().value() == id.value())
                .findFirst();
    }
}
