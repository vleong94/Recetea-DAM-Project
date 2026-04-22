package com.recetea.core.recipe.application.ports.in.report;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.io.OutputStream;

public interface IGenerateRecipeTechnicalSheetUseCase {

    void execute(RecipeId id, OutputStream outputStream);
}
