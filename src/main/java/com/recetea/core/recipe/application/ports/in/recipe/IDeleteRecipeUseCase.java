package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.domain.vo.RecipeId;

public interface IDeleteRecipeUseCase {

    void execute(RecipeId recipeId);
}
