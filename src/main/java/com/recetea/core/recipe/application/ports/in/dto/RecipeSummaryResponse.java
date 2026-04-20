package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;

public record RecipeSummaryResponse(
        RecipeId id,
        String title,
        String categoryName,
        String difficultyName,
        int prepTimeMinutes,
        int servings,
        BigDecimal averageScore,
        int totalRatings,
        String mainMediaStorageKey,
        UserId authorId
) {}
