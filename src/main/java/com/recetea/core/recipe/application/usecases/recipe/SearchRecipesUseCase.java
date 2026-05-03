package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeSummaryResponse;
import com.recetea.core.recipe.application.ports.in.dto.SearchCriteria;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.shared.domain.PageRequest;
import com.recetea.core.shared.domain.PageResponse;

/**
 * Server-side paginated search. Pure delegate to
 * {@code IRecipeRepository.searchSummaries}, which itself routes through
 * {@code RecipeSearchGateway} — that gateway pure-function-builds the WHERE
 * clause from every non-empty {@link com.recetea.core.recipe.application.ports.in.dto.SearchCriteria}
 * field and runs a count + data query against the same connection.
 *
 * <p>Empty / blank criteria fields are dropped from the predicate, so an
 * "empty criteria" call is equivalent to {@code GetAllRecipesUseCase} — the
 * dashboard takes advantage of that by passing an all-null criteria for its
 * initial bulk load.
 *
 * <p><b>ES — </b>Búsqueda paginada en servidor. Delegado puro a
 * {@code IRecipeRepository.searchSummaries}, que a su vez se enruta a
 * través de {@code RecipeSearchGateway} — ese gateway construye como
 * función pura la cláusula WHERE a partir de cada campo no vacío de
 * {@link com.recetea.core.recipe.application.ports.in.dto.SearchCriteria}
 * y ejecuta una consulta de count + datos contra la misma conexión.
 *
 * <p>Los campos de criterio vacíos / en blanco se descartan del
 * predicado, así que una llamada con "criterios vacíos" equivale a
 * {@code GetAllRecipesUseCase} — el dashboard aprovecha esto pasando
 * un criterio todo-null en su carga masiva inicial.
 */
public class SearchRecipesUseCase implements ISearchRecipesUseCase {

    private final IRecipeRepository recipeRepository;

    public SearchRecipesUseCase(IRecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public PageResponse<RecipeSummaryResponse> execute(SearchCriteria criteria, PageRequest pageRequest) {
        return recipeRepository.searchSummaries(criteria, pageRequest);
    }
}
