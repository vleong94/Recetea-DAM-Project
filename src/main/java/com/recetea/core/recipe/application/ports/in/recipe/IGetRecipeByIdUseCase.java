package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.Optional;

public interface IGetRecipeByIdUseCase {

    Optional<RecipeDetailResponse> execute(RecipeId recipeId);
}
