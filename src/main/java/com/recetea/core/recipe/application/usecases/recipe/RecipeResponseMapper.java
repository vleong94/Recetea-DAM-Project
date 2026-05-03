package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.domain.Rating;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;

/**
 * Domain-to-DTO mapper used by {@code GetRecipeByIdUseCase}. Package-private
 * because it's an internal implementation detail of the recipe-read path —
 * other use cases that need the same mapping should colocate their own
 * helpers rather than depend on this one.
 *
 * <p>Pure static, no state. Exists so the use case body stays focused on
 * orchestration (loading, ownership, transaction shape) instead of
 * record-to-record copy boilerplate.
 *
 * <p><b>ES — </b>Mapper de dominio a DTO usado por
 * {@code GetRecipeByIdUseCase}. Es package-private porque es un
 * detalle interno de la ruta de lectura de receta — otros casos de
 * uso que necesiten el mismo mapeo deberían colocar sus propios
 * helpers en lugar de depender de éste.
 *
 * <p>Static puro, sin estado. Existe para que el cuerpo del caso de
 * uso quede centrado en la orquestación (carga, propiedad, forma
 * de la transacción) en lugar de en el boilerplate de copia
 * record-a-record.
 */
final class RecipeResponseMapper {

    private RecipeResponseMapper() {}

    static RecipeDetailResponse.RatingDetail toRatingDetail(Rating r) {
        return new RecipeDetailResponse.RatingDetail(
                r.username(),
                r.score().value(),
                r.comment(),
                r.createdAt()
        );
    }

    static RecipeIngredientResponse toIngredientResponse(RecipeIngredient ri) {
        return new RecipeIngredientResponse(
                ri.ingredientId(),
                ri.unitId(),
                ri.quantity(),
                ri.ingredientName(),
                ri.unitAbbreviation()
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
