package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipesByAuthorUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;

public final class RecipeQueryWrapper implements RecipeQueryProvider {

    private final RecipeQueryContext context;

    public RecipeQueryWrapper(RecipeQueryContext context) {
        this.context = context;
    }

    @Override
    public IGetAllRecipesUseCase getAllRecipes() { return context.getAllRecipes(); }

    @Override
    public IGetRecipeByIdUseCase getRecipeById() { return context.getRecipeById(); }

    @Override
    public ISearchRecipesUseCase searchRecipes() { return context.searchRecipes(); }

    @Override
    public IGetUserFavoritesUseCase getUserFavorites() { return context.getUserFavorites(); }

    @Override
    public IGetRecipesByAuthorUseCase getRecipesByAuthor() { return context.getRecipesByAuthor(); }
}
