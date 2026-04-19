package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.ICreateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.IUpdateRecipeUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;

public final class RecipeCommandWrapper implements RecipeCommandProvider {

    private final RecipeContext context;

    public RecipeCommandWrapper(RecipeContext context) {
        this.context = context;
    }

    @Override
    public IAddRatingUseCase addRating() { return context.addRating(); }

    @Override
    public ICreateRecipeUseCase createRecipe() { return context.createRecipe(); }

    @Override
    public IUpdateRecipeUseCase updateRecipe() { return context.updateRecipe(); }

    @Override
    public IDeleteRecipeUseCase deleteRecipe() { return context.deleteRecipe(); }

    @Override
    public IGetAllCategoriesUseCase getAllCategories() { return context.getAllCategories(); }

    @Override
    public IGetAllDifficultiesUseCase getAllDifficulties() { return context.getAllDifficulties(); }

    @Override
    public IGetAllIngredientsUseCase getAllIngredients() { return context.getAllIngredients(); }

    @Override
    public IGetAllUnitsUseCase getAllUnits() { return context.getAllUnits(); }

    @Override
    public IUserSessionService sessionService() { return context.sessionService(); }
}
