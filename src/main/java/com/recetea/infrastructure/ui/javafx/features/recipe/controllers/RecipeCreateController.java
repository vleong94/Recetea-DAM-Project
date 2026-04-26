package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

public class RecipeCreateController extends BaseRecipeFormController {

    @Override
    protected RecipeId handleSave(SaveRecipeRequest request) {
        return context.createRecipe().execute(request);
    }
}