package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;

import java.math.BigDecimal;

public record RecipeIngredientResponse(
        IngredientId ingredientId,
        UnitId unitId,
        BigDecimal quantity,
        String ingredientName,
        String unitName
) {}
