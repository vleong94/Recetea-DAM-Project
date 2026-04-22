package com.recetea.core.recipe.application.usecases.report;

import com.recetea.core.recipe.application.ports.in.report.IGenerateRecipeTechnicalSheetUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.application.ports.out.report.IRecipeReportPort;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.OutputStream;

public class GenerateRecipeTechnicalSheetUseCase implements IGenerateRecipeTechnicalSheetUseCase {

    private final IRecipeRepository repository;
    private final IRecipeReportPort reportPort;

    public GenerateRecipeTechnicalSheetUseCase(IRecipeRepository repository, IRecipeReportPort reportPort) {
        this.repository = repository;
        this.reportPort = reportPort;
    }

    @Override
    public void execute(RecipeId id, OutputStream outputStream) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada: " + id));
        reportPort.generateTechnicalSheet(recipe, outputStream);
    }
}
