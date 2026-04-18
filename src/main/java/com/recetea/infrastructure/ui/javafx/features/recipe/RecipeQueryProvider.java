package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IGetAllRecipesUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ISearchRecipesUseCase;

public interface RecipeQueryProvider {
    IGetAllRecipesUseCase getAllRecipes();
    IGetRecipeByIdUseCase getRecipeById();
    ISearchRecipesUseCase searchRecipes();
}
