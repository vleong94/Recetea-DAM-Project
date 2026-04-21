package com.recetea.core.recipe.application.ports.in.interop;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.File;

public interface IImportRecipeUseCase {

    RecipeId execute(File source);
}
