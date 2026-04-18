package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;

import java.math.BigDecimal;
import java.util.List;

public record SaveRecipeRequest(
        CategoryId categoryId,
        DifficultyId difficultyId,
        String title,
        String description,
        int preparationTimeMinutes,
        int servings,
        List<IngredientRequest> ingredients,
        List<StepRequest> steps
) {
    public record IngredientRequest(
            IngredientId ingredientId,
            UnitId unitId,
            BigDecimal quantity,
            String ingredientName,
            String unitName
    ) {}

    public record StepRequest(
            int stepOrder,
            String instruction
    ) {}
}