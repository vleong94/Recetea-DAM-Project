package com.recetea.core.recipe.application.ports.in.media;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;

public interface IRemoveMediaUseCase {

    void execute(RecipeId recipeId, RecipeMediaId mediaId);
}
