package com.recetea.core.recipe.application.ports.in.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import java.util.List;

/**
 * Lists every ingredient as a {@link IngredientResponse} — used by the
 * recipe-form ingredient picker and the dashboard filter drawer's
 * multi-select. The use case maps {@code Ingredient} domain entities to
 * DTOs at the application boundary so the UI never imports domain types
 * for ingredient catalogues. Catalogue is cached at the repository layer.
 *
 * <p><b>ES — </b>Lista todos los ingredientes como
 * {@link IngredientResponse} — usado por el selector de ingredientes
 * del formulario de receta y el multi-select del cajón de filtros del
 * dashboard. El caso de uso mapea las entidades del dominio
 * {@code Ingredient} a DTOs en la frontera de aplicación, de modo que
 * la UI nunca importe tipos del dominio para los catálogos de
 * ingredientes. El catálogo se cachea en la capa de repositorio.
 */
public interface IGetAllIngredientsUseCase {

    /**
     * Returns all persisted ingredients as DTOs; never null — returns an empty list when none exist.
     *
     * <p><b>ES — </b>Devuelve todos los ingredientes persistidos como
     * DTOs; nunca null — devuelve lista vacía si no hay ninguno.
     */
    List<IngredientResponse> execute();
}
