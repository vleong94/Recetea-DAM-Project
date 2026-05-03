package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeSummariesByIdsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISuggestRecipeTitlesUseCase;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;

/** Recipe-aggregate read operations. Granular interface segregated out of {@link RecipeQueryProvider}. */
public interface IRecipeReadProvider {
    IGetAllRecipesUseCase getAllRecipes();
    IGetRecipeByIdUseCase getRecipeById();
    ISearchRecipesUseCase searchRecipes();
    IGetUserFavoritesUseCase getUserFavorites();
    IGetRecipesByAuthorUseCase getRecipesByAuthor();
    IGetRecipeSummariesByIdsUseCase getRecipeSummariesByIds();
    ISuggestRecipeTitlesUseCase suggestRecipeTitles();
}
