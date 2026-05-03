package com.recetea.core.recipe.application.ports.in.category;

import com.recetea.core.recipe.domain.Category;
import java.util.List;

/**
 * Lists every category for the recipe-form combo and the dashboard filter
 * drawer. Backed by {@code ICategoryRepository.findAll()} which caches
 * the catalogue at the JVM level — repeated invocations are essentially
 * free after the first hit.
 *
 * <p><b>ES — </b>Lista todas las categorías para el combo del formulario
 * de receta y el cajón de filtros del dashboard. Respaldado por
 * {@code ICategoryRepository.findAll()}, que cachea el catálogo a nivel
 * de JVM — las invocaciones repetidas son prácticamente gratuitas
 * después del primer hit.
 */
public interface IGetAllCategoriesUseCase {

    /**
     * Returns all persisted categories; never null — returns an empty list when none exist.
     *
     * <p><b>ES — </b>Devuelve todas las categorías persistidas; nunca
     * null — devuelve lista vacía si no hay ninguna.
     */
    List<Category> execute();
}
