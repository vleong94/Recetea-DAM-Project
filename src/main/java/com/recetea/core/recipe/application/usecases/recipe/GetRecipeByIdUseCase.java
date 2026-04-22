package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.dto.RecipeIngredientResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeIngredient;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.RecipeStep;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.domain.UserId;

import java.util.Optional;

public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;
    private final IUserSessionService sessionService;

    public GetRecipeByIdUseCase(IRecipeRepository repository, IUserSessionService sessionService) {
        this.repository = repository;
        this.sessionService = sessionService;
    }

    @Override
    public Optional<RecipeDetailResponse> execute(RecipeId recipeId) {
        Optional<UserId> currentUser = sessionService.getCurrentUserId();
        return repository.findById(recipeId)
                .map(recipe -> mapToResponse(recipe, currentUser));
    }

    private RecipeDetailResponse mapToResponse(Recipe recipe, Optional<UserId> currentUser) {
        boolean alreadyRated = currentUser
                .map(uid -> repository.hasUserRatedRecipe(uid, recipe.getId()))
                .orElse(false);

        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getAuthorId(),
                recipe.getCategory().getId(),
                recipe.getCategory().getName(),
                recipe.getDifficulty().getId(),
                recipe.getDifficulty().getName(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getPreparationTimeMinutes().value(),
                recipe.getServings().value(),
                recipe.getIngredients().stream().map(this::mapToIngredientResponse).toList(),
                recipe.getSteps().stream().map(this::mapToStepResponse).toList(),
                recipe.getAverageScore(),
                recipe.getTotalRatings(),
                recipe.getMediaItems().stream().map(this::mapToMediaResponse).toList(),
                alreadyRated
        );
    }

    private RecipeIngredientResponse mapToIngredientResponse(RecipeIngredient ri) {
        return new RecipeIngredientResponse(
                ri.getIngredientId(),
                ri.getUnitId(),
                ri.getQuantity(),
                ri.getIngredientName(),
                ri.getUnitAbbreviation()
        );
    }

    private RecipeDetailResponse.RecipeStepResponse mapToStepResponse(RecipeStep rs) {
        return new RecipeDetailResponse.RecipeStepResponse(rs.stepOrder(), rs.instruction());
    }

    private RecipeDetailResponse.RecipeMediaResponse mapToMediaResponse(RecipeMedia m) {
        return new RecipeDetailResponse.RecipeMediaResponse(
                m.id(), m.storageKey(), m.storageProvider(), m.mimeType(),
                m.sizeBytes(), m.isMain(), m.sortOrder());
    }
}
