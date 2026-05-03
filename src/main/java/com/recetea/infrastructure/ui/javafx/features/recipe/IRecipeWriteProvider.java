package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;

/** Recipe-aggregate write operations. Granular interface segregated out of {@link RecipeCommandProvider}. */
public interface IRecipeWriteProvider {
    ICreateRecipeUseCase createRecipe();
    IUpdateRecipeUseCase updateRecipe();
    IDeleteRecipeUseCase deleteRecipe();
}
