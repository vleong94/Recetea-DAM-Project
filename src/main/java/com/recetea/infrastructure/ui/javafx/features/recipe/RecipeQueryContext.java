package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeSummariesByIdsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISuggestRecipeTitlesUseCase;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;

/**
 * Read-only counterpart to {@link RecipeCommandContext} — bundles every
 * query-side use case the recipe feature consumes. Pure data DTO; the
 * matching {@link RecipeQueryWrapper} adds CID binding for production
 * use.
 *
 * <p>Note that {@code IUserSessionService} is NOT a member of this
 * record — by design, since query contexts are pure data with no
 * session dependency. The wrapper takes the session service as a
 * separate constructor arg.
 *
 * <p><b>ES — </b>Contrapartida de sólo lectura de
 * {@link RecipeCommandContext} — empaqueta cada caso de uso del lado
 * de consulta que consume el feature de recetas. DTO de datos puros;
 * el {@link RecipeQueryWrapper} correspondiente añade binding de CID
 * para uso en producción.
 *
 * <p>Nótese que {@code IUserSessionService} NO es miembro de este
 * record — por diseño, ya que los contextos de consulta son datos
 * puros sin dependencia de sesión. El wrapper recibe el servicio de
 * sesión como argumento de constructor separado.
 */
public record RecipeQueryContext(
        IGetAllRecipesUseCase getAllRecipes,
        IGetRecipeByIdUseCase getRecipeById,
        ISearchRecipesUseCase searchRecipes,
        IGetUserFavoritesUseCase getUserFavorites,
        IGetRecipesByAuthorUseCase getRecipesByAuthor,
        IGetRecipeSummariesByIdsUseCase getRecipeSummariesByIds,
        ISuggestRecipeTitlesUseCase suggestRecipeTitles
) implements RecipeQueryProvider {}
