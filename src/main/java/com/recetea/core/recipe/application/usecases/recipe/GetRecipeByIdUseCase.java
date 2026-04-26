package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.domain.UserId;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository repository;
    private final IUserSessionService sessionService;
    // Virtual-thread executor — the blocking JDBC call parks the VT without occupying a carrier.
    private final Executor executor;

    public GetRecipeByIdUseCase(IRecipeRepository repository, IUserSessionService sessionService,
                                Executor executor) {
        this.repository     = repository;
        this.sessionService = sessionService;
        this.executor       = executor;
    }

    /**
     * Synchronous entry point kept for callers that already run on a background / virtual thread
     * (e.g. inside a JavaFX {@code Task}).
     */
    @Override
    public Optional<RecipeDetailResponse> execute(RecipeId recipeId) {
        Optional<UserId> currentUser = sessionService.getCurrentUserId();
        return repository.findById(recipeId)
                .map(recipe -> mapToResponse(recipe, currentUser));
    }

    /**
     * Async entry point — submits the blocking repository call to the virtual-thread executor
     * so the caller (e.g. the FX thread) is never blocked.
     */
    public CompletableFuture<Optional<RecipeDetailResponse>> executeAsync(RecipeId recipeId) {
        return CompletableFuture.supplyAsync(() -> execute(recipeId), executor);
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
                recipe.getIngredients().stream().map(RecipeResponseMapper::toIngredientResponse).toList(),
                recipe.getSteps().stream().map(RecipeResponseMapper::toStepResponse).toList(),
                recipe.getAverageScore(),
                recipe.getTotalRatings(),
                recipe.getMediaItems().stream().map(RecipeResponseMapper::toMediaResponse).toList(),
                alreadyRated,
                recipe.getRatings().stream().map(RecipeResponseMapper::toRatingDetail).toList()
        );
    }
}
