package com.recetea.infrastructure.ui.javafx.features.recipe;

/**
 * Read-side provider for the recipe aggregate. Currently inherits only
 * {@link IRecipeReadProvider}; declares no methods of its own.
 *
 * <p>Catalogue reads ({@link ITaxonomyQueryProvider},
 * {@link IIngredientQueryProvider}) are reachable through
 * {@link RecipeCommandProvider} because the recipe-form controllers
 * (which need both writes AND catalogue reads to populate ComboBoxes)
 * receive a single command-side context. New code that wants catalogue
 * reads in isolation should depend on those granular interfaces directly.
 */
public interface RecipeQueryProvider extends IRecipeReadProvider {
}
