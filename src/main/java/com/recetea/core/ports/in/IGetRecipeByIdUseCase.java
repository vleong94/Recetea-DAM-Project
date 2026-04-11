package com.recetea.core.ports.in;

import com.recetea.core.domain.Recipe;
import java.util.Optional;

public interface IGetRecipeByIdUseCase {
    Optional<Recipe> execute(int recipeId);
}