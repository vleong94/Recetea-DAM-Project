package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.domain.vo.RecipeId;

public interface IToggleFavoriteUseCase {
    void execute(RecipeId recipeId);
}
