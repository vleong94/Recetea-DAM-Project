package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeSummariesByIdsUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.List;

/**
 * Hydration helper: turns a list of {@link RecipeId}s into the matching
 * summary DTOs. Used by {@code UserProfileController} to render the
 * "Favourites" tab — the social context returns ids only (it knows nothing
 * about recipe DTOs), so this use case bridges the two bounded contexts
 * inside the application layer.
 *
 * <p>Empty input short-circuits to an empty result without touching the DB —
 * the repository's {@code findSummariesByIds} would build a no-op
 * {@code IN ()} clause that PostgreSQL rejects, so the guard here is a
 * correctness requirement, not just an optimisation.
 *
 * <p><b>ES — </b>Helper de hidratación: convierte una lista de
 * {@link RecipeId} en los DTOs resumen correspondientes. Usado por
 * {@code UserProfileController} para renderizar la pestaña de
 * "Favoritos" — el contexto social devuelve sólo ids (no sabe nada
 * sobre los DTOs de receta), así que este caso de uso une los dos
 * contextos delimitados dentro de la capa de aplicación.
 *
 * <p>La entrada vacía corto-circuita a un resultado vacío sin tocar
 * la BD — el {@code findSummariesByIds} del repositorio construiría
 * una cláusula {@code IN ()} no-op que PostgreSQL rechaza, así que
 * el guard aquí es un requisito de corrección, no sólo una
 * optimización.
 */
public class GetRecipeSummariesByIdsUseCase implements IGetRecipeSummariesByIdsUseCase {

    private final IRecipeRepository recipeRepository;

    public GetRecipeSummariesByIdsUseCase(IRecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<RecipeSummaryResponse> execute(List<RecipeId> ids) {
        if (ids.isEmpty()) return List.of();
        return recipeRepository.findSummariesByIds(ids);
    }
}
