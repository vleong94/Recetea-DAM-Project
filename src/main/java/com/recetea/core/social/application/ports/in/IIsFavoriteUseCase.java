package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.domain.vo.RecipeId;

public interface IIsFavoriteUseCase {
    boolean execute(RecipeId recipeId);
}
