package com.recetea.core.recipe.application.ports.in.media;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.InputStream;

public interface IAttachMediaUseCase {

    void execute(RecipeId recipeId, InputStream data, String originalName);
}
