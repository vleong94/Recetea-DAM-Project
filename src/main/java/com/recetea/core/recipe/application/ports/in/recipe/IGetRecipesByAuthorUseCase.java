package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;
import com.recetea.core.user.domain.UserId;

/**
 * Paged listing scoped to a single author — backs the user-profile
 * "my recipes" tab and the public author drilldown.
 *
 * <p>No ownership check — the result set is filtered at the SQL layer by
 * {@code authorId}, so any caller can list any user's recipes (they're
 * public). Returns an empty page when the author exists but has no
 * recipes, never null.
 *
 * <p><b>ES — </b>Listado paginado acotado a un único autor — respalda la
 * pestaña "mis recetas" del perfil de usuario y el drilldown público de
 * autor.
 *
 * <p>Sin comprobación de propiedad — el conjunto de resultados se filtra
 * en la capa SQL por {@code authorId}, así que cualquier llamador puede
 * listar las recetas de cualquier usuario (son públicas). Devuelve una
 * página vacía cuando el autor existe pero no tiene recetas, nunca null.
 */
public interface IGetRecipesByAuthorUseCase {

    PageResponse<RecipeSummaryResponse> execute(UserId authorId, PageRequest page);
}
