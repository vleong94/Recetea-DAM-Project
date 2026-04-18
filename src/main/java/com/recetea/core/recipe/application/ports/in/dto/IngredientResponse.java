package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.IngredientId;

public record IngredientResponse(
        IngredientId id,
        String name
) {}
