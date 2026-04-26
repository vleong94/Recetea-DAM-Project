package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;

final class RecipeResponseMapper {

    private RecipeResponseMapper() {}

    static RecipeDetailResponse.RatingDetail toRatingDetail(Rating r) {
        return new RecipeDetailResponse.RatingDetail(
                r.getUsername(),
                r.getScore().value(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    static RecipeIngredientResponse toIngredientResponse(RecipeIngredient ri) {
        return new RecipeIngredientResponse(
                ri.getIngredientId(),
                ri.getUnitId(),
                ri.getQuantity(),
                ri.getIngredientName(),
                ri.getUnitAbbreviation()
        );
    }

    static RecipeDetailResponse.RecipeStepResponse toStepResponse(RecipeStep rs) {
        return new RecipeDetailResponse.RecipeStepResponse(rs.stepOrder(), rs.instruction());
    }

    static RecipeDetailResponse.RecipeMediaResponse toMediaResponse(RecipeMedia m) {
        return new RecipeDetailResponse.RecipeMediaResponse(
                m.id(), m.storageKey(), m.storageProvider(), m.mimeType(),
                m.sizeBytes(), m.isMain(), m.sortOrder()
        );
    }
}
