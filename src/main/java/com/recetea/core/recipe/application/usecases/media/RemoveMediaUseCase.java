package com.recetea.core.recipe.application.usecases.media;

import com.recetea.core.recipe.application.ports.in.media.IRemoveMediaUseCase;
import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.recipe.domain.vo.RecipeMediaId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;

public class RemoveMediaUseCase implements IRemoveMediaUseCase {

    private final IRecipeRepository recipeRepository;
    private final IMediaStorageService storageService;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public RemoveMediaUseCase(IRecipeRepository recipeRepository,
                              IMediaStorageService storageService,
                              ITransactionManager transactionManager,
                              IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.storageService = storageService;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId, RecipeMediaId mediaId) {
        // Commit DB changes first, then delete the physical file.
        // If DB fails → file is untouched, state is consistent.
        // If file deletion fails → file is orphaned but DB is consistent (acceptable tradeoff).
        String storageKey = transactionManager.execute(() -> {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Receta no encontrada con ID: " + recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "El usuario " + currentUser.value() + " no tiene permiso para modificar esta receta.");
            }

            String key = recipe.getMediaItems().stream()
                    .filter(m -> mediaId.equals(m.id()))
                    .map(RecipeMedia::storageKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Elemento multimedia no encontrado con ID: " + mediaId.value()));

            recipe.removeMedia(mediaId);
            recipeRepository.update(recipe);
            return key;
        });

        storageService.delete(storageKey);
    }
}
