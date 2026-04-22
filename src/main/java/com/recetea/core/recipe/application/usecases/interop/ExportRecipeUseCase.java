package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.File;

public class ExportRecipeUseCase implements IExportRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final IRecipeInteropPort interopPort;

    public ExportRecipeUseCase(IRecipeRepository recipeRepository, IRecipeInteropPort interopPort) {
        this.recipeRepository = recipeRepository;
        this.interopPort = interopPort;
    }

    @Override
    public void execute(RecipeId recipeId, File destination) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Receta no encontrada con ID: " + recipeId.value()));
        interopPort.exportRecipe(recipe, destination);
    }
}
