package com.recetea.core.ports.in.recipe;

import com.recetea.core.domain.Recipe;
import java.util.Optional;

public interface IGetRecipeByIdUseCase {
    Optional<Recipe> execute(int recipeId);
}