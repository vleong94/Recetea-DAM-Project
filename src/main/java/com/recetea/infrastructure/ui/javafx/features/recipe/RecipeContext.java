package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.*;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.shared.application.ports.in.IUserSessionService;

public record RecipeContext(
        IAddRatingUseCase addRating,
        ICreateRecipeUseCase createRecipe,
        IGetAllRecipesUseCase getAllRecipes,
        IGetRecipeByIdUseCase getRecipeById,
        ISearchRecipesUseCase searchRecipes,
        IUpdateRecipeUseCase updateRecipe,
        IDeleteRecipeUseCase deleteRecipe,
        IGetAllIngredientsUseCase getAllIngredients,
        IGetAllUnitsUseCase getAllUnits,
        IGetAllCategoriesUseCase getAllCategories,
        IGetAllDifficultiesUseCase getAllDifficulties,
        IUserSessionService sessionService
) implements RecipeQueryProvider, RecipeCommandProvider {}
