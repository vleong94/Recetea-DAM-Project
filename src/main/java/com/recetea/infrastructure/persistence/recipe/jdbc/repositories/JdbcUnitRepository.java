package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.unit.IUnitRepository;
import com.recetea.core.recipe.domain.Unit;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.UnitMapper;
import java.util.List;

/**
 * Adaptador JDBC para el maestro de unidades de medida.
 * Provee acceso al catálogo global de escalas, garantizando que el mapeo
 * relacional-objeto sea consistente mediante el uso de mappers estandarizados.
 */
public class JdbcUnitRepository extends BaseJdbcRepository implements IUnitRepository {

    private static final String SELECT_ALL = "SELECT id_unit, name, abbreviation FROM unit_measures ORDER BY name ASC";
    private final UnitMapper mapper = new UnitMapper();

    public JdbcUnitRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public List<Unit> findAll() {
        return queryForList(SELECT_ALL, mapper);
    }
}