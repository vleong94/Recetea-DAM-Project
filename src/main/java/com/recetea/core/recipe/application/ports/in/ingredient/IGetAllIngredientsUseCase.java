package com.recetea.core.recipe.application.ports.in.ingredient;

import com.recetea.core.recipe.application.ports.in.dto.IngredientResponse;
import java.util.List;

public interface IGetAllIngredientsUseCase {

    /** Returns all persisted ingredients as DTOs; never null — returns an empty list when none exist. */
    List<IngredientResponse> execute();
}
