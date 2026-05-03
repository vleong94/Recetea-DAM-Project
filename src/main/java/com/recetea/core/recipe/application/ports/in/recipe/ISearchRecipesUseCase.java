package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

/**
 * Server-side recipe search with composable predicates. The implementation
 * delegates to {@code RecipeSearchGateway.searchSummaries(...)}, which pure-
 * function-builds a WHERE clause from every non-empty {@link SearchCriteria}
 * field (title ILIKE / category equals / ingredient AND-match via EXISTS +
 * HAVING / author / minScore / etc.) and runs two queries inside the same
 * connection: a count for pagination totals, and a {@code LIMIT/OFFSET}
 * data fetch.
 *
 * <p>The dashboard does <em>not</em> currently page through search results —
 * it loads up to 200 rows once and applies further client-side filtering
 * via {@code RecipeSearchViewModel.predicateFor}. So this port is the
 * "initial data load" path; real-time filter narrowing happens in-memory.
 *
 * <p><b>ES — </b>Búsqueda de recetas en el servidor con predicados
 * componibles. La implementación delega en
 * {@code RecipeSearchGateway.searchSummaries(...)}, que construye como
 * función pura una cláusula WHERE a partir de cada campo no vacío de
 * {@link SearchCriteria} (title ILIKE / category equals / coincidencia
 * AND de ingredientes vía EXISTS + HAVING / autor / minScore / etc.) y
 * ejecuta dos consultas dentro de la misma conexión: un count para los
 * totales de paginación y un fetch de datos {@code LIMIT/OFFSET}.
 *
 * <p>Actualmente el dashboard <em>no</em> pagina los resultados de
 * búsqueda — carga hasta 200 filas una sola vez y aplica filtrado
 * adicional en el cliente vía {@code RecipeSearchViewModel.predicateFor}.
 * Así que este puerto es la ruta de "carga inicial de datos"; el
 * estrechamiento de filtros en tiempo real ocurre en memoria.
 */
public interface ISearchRecipesUseCase {
    PageResponse<RecipeSummaryResponse> execute(SearchCriteria criteria, PageRequest pageRequest);
}
