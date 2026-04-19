package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.Score;

public record AddRatingRequest(
        RecipeId recipeId,
        Score score,
        String comment
) {}
