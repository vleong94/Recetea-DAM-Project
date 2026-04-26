package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.interop.IRecipeInteropPort;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ExportRecipeUseCase implements IExportRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExportRecipeUseCase.class);

    private final IRecipeRepository recipeRepository;
    private final IRecipeInteropPort interopPort;

    public ExportRecipeUseCase(IRecipeRepository recipeRepository, IRecipeInteropPort interopPort) {
        this.recipeRepository = recipeRepository;
        this.interopPort = interopPort;
    }

    @Override
    public void execute(RecipeId recipeId, File destination) {
        log.info("Exporting recipe: {}", recipeId.value());

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipe not found with ID: " + recipeId.value()));
        interopPort.exportRecipe(recipe, destination);

        log.info("Recipe {} exported to: '{}'", recipeId.value(), destination.getName());
    }
}
