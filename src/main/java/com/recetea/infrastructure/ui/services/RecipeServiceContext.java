package com.recetea.infrastructure.ui.services;

import com.recetea.core.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.ports.in.unit.IGetAllUnitsUseCase;
import com.recetea.core.ports.in.recipe.*;

/**
 * Context Object: Agrupador de Casos de Uso.
 * Esta clase permite que los controladores reciban un único objeto con toda la
 * lógica de negocio inyectada, simplificando su mantenimiento.
 */
public record RecipeServiceContext(
        ICreateRecipeUseCase createRecipe,
        IGetAllRecipesUseCase getAllRecipes,
        IGetRecipeByIdUseCase getRecipeById,
        IUpdateRecipeUseCase updateRecipe,
        IDeleteRecipeUseCase deleteRecipe,
        IGetAllIngredientsUseCase getAllIngredients,
        IGetAllUnitsUseCase getAllUnits
) {}