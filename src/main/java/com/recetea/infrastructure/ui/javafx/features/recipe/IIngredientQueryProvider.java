package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.ingredient.IGetAllIngredientsUseCase;

/**
 * Ingredient catalogue read. Separate from {@link ITaxonomyQueryProvider}
 * because ingredients have a richer schema (category + name + future allergens)
 * and may grow to a write-capable interface independently of taxonomy.
 */
public interface IIngredientQueryProvider {
    IGetAllIngredientsUseCase getAllIngredients();
}
