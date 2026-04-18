package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

public interface IUpdateRecipeUseCase {

    void execute(RecipeId recipeId, SaveRecipeRequest request);
}
