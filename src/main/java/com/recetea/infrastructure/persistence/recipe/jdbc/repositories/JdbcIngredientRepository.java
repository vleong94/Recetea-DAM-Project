package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.ingredient.IIngredientRepository;
import com.recetea.core.recipe.domain.Ingredient;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.IngredientMapper;
import java.util.List;

/**
 * Adaptador JDBC para la gestión de ingredientes.
 * Implementa la recuperación masiva del catálogo delegando el control de flujo
 * a la clase base, manteniendo la lógica centrada exclusivamente en el SQL.
 */
public class JdbcIngredientRepository extends BaseJdbcRepository implements IIngredientRepository {

    private static final String SELECT_ALL = "SELECT id_ingredient, ing_category_id, name FROM ingredients ORDER BY name ASC";
    private final IngredientMapper mapper = new IngredientMapper();

    public JdbcIngredientRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public List<Ingredient> findAll() {
        return queryForList(SELECT_ALL, mapper);
    }
}