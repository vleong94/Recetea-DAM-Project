package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.difficulty.IDifficultyRepository;
import com.recetea.core.recipe.domain.Difficulty;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.DifficultyMapper;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC para el catálogo de dificultades.
 * Extiende la funcionalidad base para realizar consultas seguras, utilizando
 * el mapper especializado para la reconstrucción de las entidades de dominio.
 */
public class JdbcDifficultyRepository extends BaseJdbcRepository implements IDifficultyRepository {

    private static final String SELECT_ALL = "SELECT id_difficulty, name FROM difficulties ORDER BY id_difficulty ASC";
    private static final String SELECT_BY_ID = "SELECT id_difficulty, name FROM difficulties WHERE id_difficulty = ?";
    private final DifficultyMapper mapper = new DifficultyMapper();

    public JdbcDifficultyRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public List<Difficulty> findAll() {
        return queryForList(SELECT_ALL, mapper);
    }

    @Override
    public Optional<Difficulty> findById(int id) {
        return queryForObject(SELECT_BY_ID, mapper, id);
    }
}