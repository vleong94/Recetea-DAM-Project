package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.CategoryMapper;

import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository extends BaseJdbcRepository implements ICategoryRepository {

    private static final String SELECT_ALL = "SELECT id_category, name FROM categories ORDER BY name ASC";
    private static final String SELECT_BY_ID = "SELECT id_category, name FROM categories WHERE id_category = ?";
    private final CategoryMapper mapper = new CategoryMapper();

    public JdbcCategoryRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public List<Category> findAll() {
        return queryForList(SELECT_ALL, mapper);
    }

    @Override
    public Optional<Category> findById(int id) {
        return queryForObject(SELECT_BY_ID, mapper, id);
    }
}