package com.recetea.infrastructure.ui.javafx.recipe;

import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;
import com.recetea.core.recipe.application.ports.in.recipe.*;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;

/**
 * Contexto de ejecución para la funcionalidad de Recetas.
 * Implementa el patrón Context Object mediante una estructura inmutable (record)
 * para agrupar los puertos de entrada (Use Cases) definidos en la capa de dominio.
 *
 * Actúa como un contenedor de dependencias inyectable. Su propósito arquitectónico
 * es centralizar el acceso a la lógica de negocio, evitando la sobrecarga de parámetros
 * en la inicialización de los controladores de vista y asegurando el aislamiento
 * estricto entre la interfaz de usuario y las reglas del Core.
 */
public record RecipeContext(
        ICreateRecipeUseCase createRecipe,
        IGetAllRecipesUseCase getAllRecipes,
        IGetRecipeByIdUseCase getRecipeById,
        IUpdateRecipeUseCase updateRecipe,
        IDeleteRecipeUseCase deleteRecipe,
        IGetAllIngredientsUseCase getAllIngredients,
        IGetAllUnitsUseCase getAllUnits
) {}