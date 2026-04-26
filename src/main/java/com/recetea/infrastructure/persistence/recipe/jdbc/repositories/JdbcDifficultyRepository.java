package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.DifficultyMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class JdbcDifficultyRepository extends BaseJdbcRepository implements IDifficultyRepository {

    private static final String SELECT_ALL = "SELECT id_difficulty, level_name FROM difficulties ORDER BY id_difficulty ASC";
    private final DifficultyMapper mapper = new DifficultyMapper();
    private final AtomicReference<List<Difficulty>> cache = new AtomicReference<>();

    public JdbcDifficultyRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
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
                .filter(d -> d.getId().value() == id.value())
                .findFirst();
    }
}
