package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.recipe.IDeleteRecipeUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteRecipeUseCase implements IDeleteRecipeUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteRecipeUseCase.class);

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
        log.info("Deleting recipe: {}", recipeId.value());

        transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RecipeNotFoundException(recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "User " + currentUser.value() + " is not authorized to delete recipe " + recipeId.value() + ".");
            }

            recipeRepository.delete(recipeId);
        });

        log.info("Recipe {} deleted successfully.", recipeId.value());
    }
}
