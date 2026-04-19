package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;

public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {

    private final IRecipeRepository recipeRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public DeleteRecipeUseCase(IRecipeRepository recipeRepository,
                               ITransactionManager transactionManager,
                               IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId) {
        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada con ID: " + recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "El usuario " + currentUser.value() + " no tiene permiso para eliminar esta receta.");
            }

            recipeRepository.delete(recipeId);
        });
    }
}
