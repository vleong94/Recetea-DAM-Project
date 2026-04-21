package com.recetea.core.recipe.application.usecases.interop;

import com.recetea.core.recipe.application.ports.in.interop.IExportRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.infrastructure.interop.xml.XmlInteropAdapter;

import java.io.File;

public class ExportRecipeUseCase implements IExportRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final XmlInteropAdapter xmlAdapter;

    public ExportRecipeUseCase(IRecipeRepository recipeRepository, XmlInteropAdapter xmlAdapter) {
        this.recipeRepository = recipeRepository;
        this.xmlAdapter = xmlAdapter;
    }

    @Override
    public void execute(RecipeId recipeId, File destination) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Receta no encontrada con ID: " + recipeId.value()));
        xmlAdapter.toFile(recipe, destination);
    }
}
