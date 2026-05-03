package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

/**
 * Paginated listing of every recipe — the dashboard's bulk-load entry point
 * (currently called once at mount with {@code PageRequest(0, 200)} so the
 * client-side {@code RecipeSearchViewModel} has the full set to filter against).
 *
 * <p>Pure delegate to {@code IRecipeRepository.findAllSummaries} — no
 * authorisation, no transaction. Read-only over the denormalised summary
 * projection ({@code RecipeSqlRegistry.SELECT_SUMMARIES}); the listing path
 * never JOINs the {@code ratings} table because {@code recipes.average_score}
 * and {@code recipes.total_ratings} are kept up-to-date by the rating-write path.
 *
 * <p><b>ES — </b>Listado paginado de todas las recetas — el punto de
 * entrada de carga masiva del dashboard (actualmente se llama una vez
 * al montarlo con {@code PageRequest(0, 200)} para que el
 * {@code RecipeSearchViewModel} del cliente tenga el conjunto completo
 * contra el que filtrar).
 *
 * <p>Delegado puro a {@code IRecipeRepository.findAllSummaries} — sin
 * autorización, sin transacción. Sólo lectura sobre la proyección
 * resumen desnormalizada ({@code RecipeSqlRegistry.SELECT_SUMMARIES});
 * la ruta de listado nunca hace JOIN con la tabla {@code ratings}
 * porque {@code recipes.average_score} y {@code recipes.total_ratings}
 * los mantiene al día la ruta de escritura de valoraciones.
 */
public class GetAllRecipesUseCase implements IGetAllRecipesUseCase {

    private final IRecipeRepository repository;

    public GetAllRecipesUseCase(IRecipeRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<RecipeSummaryResponse> execute(PageRequest pageRequest) {
        return repository.findAllSummaries(pageRequest);
    }
}
