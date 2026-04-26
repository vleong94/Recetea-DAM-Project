package com.recetea.core.recipe.application.ports.in.dto;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.DifficultyId;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.core.user.domain.UserId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailResponse(
        RecipeId id,
        UserId userId,
        CategoryId categoryId,
        String categoryName,
        DifficultyId difficultyId,
        String difficultyName,
        String title,
        String description,
        int prepTimeMinutes,
        int servings,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps,
        BigDecimal averageScore,
        int totalRatings,
        List<RecipeMediaResponse> media,
        boolean alreadyRatedByCurrentUser,
        List<RatingDetail> ratings
) {
    public record RecipeStepResponse(
            int stepOrder,
            String instruction
    ) {}

    public record RecipeMediaResponse(
            RecipeMediaId id,
            String storageKey,
            String storageProvider,
            String mimeType,
            long sizeBytes,
            boolean isMain,
            int sortOrder
    ) {}

    /** Projection of a single rating for social display. username may be null if the voter
     *  account has been deleted since the rating was cast. */
    public record RatingDetail(
            String username,
            int score,
            String comment,
            LocalDateTime date
    ) {}
}
