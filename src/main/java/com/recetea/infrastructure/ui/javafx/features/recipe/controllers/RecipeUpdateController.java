package com.recetea.infrastructure.ui.javafx.features.recipe.controllers;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.stream.Collectors;

public class RecipeUpdateController extends BaseRecipeFormController {

    private RecipeId currentRecipeId;

    public void loadRecipeData(RecipeDetailResponse recipe) {
        this.currentRecipeId = recipe.id();

        headerComponent.setData(
                recipe.title(),
                recipe.description(),
                recipe.prepTimeMinutes(),
                recipe.servings(),
                recipe.categoryId(),
                recipe.difficultyId()
        );

        ingredientTableComponent.loadExistingIngredients(
                recipe.ingredients().stream()
                        .map(i -> new SaveRecipeRequest.IngredientRequest(
                                i.ingredientId(),
                                i.unitId(),
                                i.quantity(),
                                i.ingredientName(),
                                i.unitName()))
                        .collect(Collectors.toList())
        );

        stepTableComponent.loadSteps(recipe.steps());
    }

    @Override
    protected void handleSave(SaveRecipeRequest request) {
        context.updateRecipe().execute(currentRecipeId, request);
    }
}
