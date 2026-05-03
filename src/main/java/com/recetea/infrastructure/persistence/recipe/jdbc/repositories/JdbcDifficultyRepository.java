package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.shared.application.ports.out.IMetricsPort;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.DifficultyMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JDBC adapter for the difficulty catalogue. Same caching pattern as
 * {@link JdbcCategoryRepository} — the row set is enum-shaped (4 rows)
 * and never changes at runtime, so a one-shot CAS-bound cache is the
 * cheapest correct strategy. The select is ordered by {@code difficulty_id}
 * (not name) so the canonical Easy → Expert progression is preserved.
 *
 * <p><b>ES — </b>Adaptador JDBC para el catálogo de dificultad. Mismo
 * patrón de caché que {@link JdbcCategoryRepository} — el conjunto
 * de filas tiene forma de enum (4 filas) y nunca cambia en runtime,
 * así que una caché one-shot ligada por CAS es la estrategia
 * correcta más barata. El select se ordena por {@code difficulty_id}
 * (no por nombre) para preservar la progresión canónica
 * Fácil → Experto.
 */
public class JdbcDifficultyRepository extends BaseJdbcRepository implements IDifficultyRepository {

    private static final String SELECT_ALL = "SELECT difficulty_id, difficulty_level FROM difficulties ORDER BY difficulty_id ASC";
    private final DifficultyMapper mapper = new DifficultyMapper();
    private final AtomicReference<List<Difficulty>> cache = new AtomicReference<>();

    public JdbcDifficultyRepository(JdbcTransactionManager transactionManager, IMetricsPort metricsPort) {
        super(transactionManager, metricsPort);
    }

    @Override
    public List<Difficulty> findAll() {
        List<Difficulty> cached = cache.get();
        if (cached != null) return cached;
        List<Difficulty> loaded = queryForList(SELECT_ALL, mapper);
        cache.compareAndSet(null, List.copyOf(loaded));
        return cache.get();
    }

    @Override
    public Optional<Difficulty> findById(DifficultyId id) {
        return findAll().stream()
                .filter(d -> d.id().value() == id.value())
                .findFirst();
    }
}
