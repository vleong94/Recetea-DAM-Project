package com.recetea.core.recipe.application.usecases.category;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.out.category.ICategoryRepository;
import com.recetea.core.recipe.domain.Category;

import java.util.List;

/**
 * Returns the full list of categories — fixed-catalogue master data, never
 * mutates at runtime. Cached lazily inside
 * {@code JdbcCategoryRepository}'s {@code AtomicReference<List<Category>>}
 * so subsequent calls hit the in-memory list without touching the DB.
 *
 * <p>Used to populate the recipe-form category combo, the dashboard category
 * filter, and the {@code XmlInteropAdapter}'s name → entity lookup table.
 * Returns the cached list directly (already unmodifiable via {@code List.copyOf}
 * inside the repository) so callers can iterate or stream safely.
 *
 * <p><b>ES — </b>Devuelve la lista completa de categorías — datos
 * maestros de catálogo fijo, nunca mutan en tiempo de ejecución.
 * Cacheada perezosamente dentro del
 * {@code AtomicReference<List<Category>>} de
 * {@code JdbcCategoryRepository}, de modo que las llamadas posteriores
 * van a la lista en memoria sin tocar la BD.
 *
 * <p>Usado para poblar el combo de categoría del formulario de receta,
 * el filtro de categoría del dashboard y la tabla de lookup
 * nombre → entidad de {@code XmlInteropAdapter}. Devuelve la lista
 * cacheada directamente (ya inmutable vía {@code List.copyOf} dentro
 * del repositorio) para que los llamadores puedan iterar o hacer
 * stream con seguridad.
 */
public class GetAllCategoriesUseCase implements IGetAllCategoriesUseCase {

    private final ICategoryRepository repository;

    public GetAllCategoriesUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Category> execute() {
        return repository.findAll();
    }
}
