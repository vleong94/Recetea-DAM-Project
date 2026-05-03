package com.recetea.infrastructure.ui.javafx.features.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;

/** Rating write operations. Granular interface segregated out of {@link RecipeCommandProvider}. */
public interface IRatingWriteProvider {
    IAddRatingUseCase addRating();
}
