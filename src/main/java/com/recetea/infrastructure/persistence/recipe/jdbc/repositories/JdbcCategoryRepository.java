package com.recetea.infrastructure.persistence.recipe.jdbc.repositories;

import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;
import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.infrastructure.persistence.recipe.jdbc.JdbcTransactionManager;
import com.recetea.infrastructure.persistence.recipe.jdbc.mappers.CategoryMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class JdbcCategoryRepository extends BaseJdbcRepository implements ICategoryRepository {

    private static final String SELECT_ALL = "SELECT id_category, name FROM categories ORDER BY name ASC";
    private final CategoryMapper mapper = new CategoryMapper();
    private final AtomicReference<List<Category>> cache = new AtomicReference<>();

    public JdbcCategoryRepository(JdbcTransactionManager transactionManager) {
        super(transactionManager);
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
                .filter(c -> c.getId().value() == id.value())
                .findFirst();
    }
}
