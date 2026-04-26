package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;

public class AddRatingUseCase implements IAddRatingUseCase {

    private final IRecipeRepository recipeRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public AddRatingUseCase(IRecipeRepository recipeRepository,
                            ITransactionManager transactionManager,
                            IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(AddRatingRequest request) {
        transactionManager.execute(() -> {
            var recipe = recipeRepository.findById(request.recipeId())
                    .orElseThrow(() -> new RecipeNotFoundException(request.recipeId().value()));
            var voterId = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            recipe.addRating(voterId, request.score(), request.comment());
            recipeRepository.update(recipe);
            return null;
        });
    }
}
