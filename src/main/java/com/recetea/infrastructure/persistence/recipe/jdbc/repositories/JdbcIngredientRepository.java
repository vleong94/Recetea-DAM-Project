package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.IngredientMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JDBC adapter for the ingredient catalogue. Same one-shot caching as
 * the other catalogue adapters — the seeded set is large (~300 rows in
 * production) but stable. {@link IIngredientRepository} only declares
 * {@link #findAll()}, so there is no by-id lookup here; callers that
 * need one (e.g. the search predicate composer) iterate the cached list.
 *
 * <p><b>ES — </b>Adaptador JDBC para el catálogo de ingredientes.
 * Mismo cacheado one-shot que los otros adaptadores de catálogo —
 * el conjunto semilla es grande (~300 filas en producción) pero
 * estable. {@link IIngredientRepository} sólo declara
 * {@link #findAll()}, así que no hay lookup por id aquí; los
 * llamadores que lo necesiten (p. ej. el compositor de predicados
 * de búsqueda) iteran la lista cacheada.
 */
public class JdbcIngredientRepository extends BaseJdbcRepository implements IIngredientRepository {

    private static final String SELECT_ALL = "SELECT ingredient_id, ingredient_category_id, name FROM ingredients ORDER BY name ASC";
    private final IngredientMapper mapper = new IngredientMapper();
    private final AtomicReference<List<Ingredient>> cache = new AtomicReference<>();

    public JdbcIngredientRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public List<Ingredient> findAll() {
        List<Ingredient> cached = cache.get();
        if (cached != null) return cached;
        List<Ingredient> loaded = queryForList(SELECT_ALL, mapper);
        cache.compareAndSet(null, List.copyOf(loaded));
        return cache.get();
    }
}
