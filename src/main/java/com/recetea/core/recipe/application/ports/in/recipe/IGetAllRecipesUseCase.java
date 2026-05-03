package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

/**
 * Paged listing of every recipe — the dashboard's bulk-load entry point.
 *
 * <p>Currently invoked once at dashboard mount with {@code PageRequest(0, 200)}
 * so {@code RecipeSearchViewModel} has the full set to filter against
 * client-side; the page-style signature stays in place to keep the door
 * open for server-side pagination once the catalogue grows beyond a few
 * hundred entries.
 *
 * <p><b>ES — </b>Listado paginado de todas las recetas — el punto de
 * entrada de carga masiva del dashboard.
 *
 * <p>Actualmente se invoca una vez al montar el dashboard con
 * {@code PageRequest(0, 200)} para que {@code RecipeSearchViewModel}
 * tenga el conjunto completo contra el que filtrar en cliente; la firma
 * con estilo paginado se mantiene para dejar abierta la puerta a la
 * paginación en servidor cuando el catálogo crezca más allá de unos
 * pocos centenares de entradas.
 */
public interface IGetAllRecipesUseCase {
    PageResponse<RecipeSummaryResponse> execute(PageRequest pageRequest);
}
