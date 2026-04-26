package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.UnitMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JdbcUnitRepository extends BaseJdbcRepository implements IUnitRepository {

    private static final String SELECT_ALL = "SELECT id_unit, name, abbreviation FROM unit_measures ORDER BY name ASC";
    private final UnitMapper mapper = new UnitMapper();
    private final AtomicReference<List<Unit>> cache = new AtomicReference<>();

    public JdbcUnitRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
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
