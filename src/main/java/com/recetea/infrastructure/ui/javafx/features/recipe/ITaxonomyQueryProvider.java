package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.category.IGetAllCategoriesUseCase;
import com.recetea.core.recipe.application.ports.in.difficulty.IGetAllDifficultiesUseCase;
import com.recetea.core.recipe.application.ports.in.unit.IGetAllUnitsUseCase;

/**
 * Taxonomy catalogue reads — categories, difficulties, units of measure.
 * These are immutable at runtime (see CLAUDE.md) so consumers can cache the
 * returned lists indefinitely.
 */
public interface ITaxonomyQueryProvider {
    IGetAllCategoriesUseCase getAllCategories();
    IGetAllDifficultiesUseCase getAllDifficulties();
    IGetAllUnitsUseCase getAllUnits();
}
