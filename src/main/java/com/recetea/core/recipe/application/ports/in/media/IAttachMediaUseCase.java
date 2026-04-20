package com.recetea.core.recipe.application.ports.in.media;

import com.recetea.core.recipe.domain.vo.RecipeId;

public interface IAttachMediaUseCase {

    void execute(RecipeId recipeId, byte[] data, String originalName);
}
