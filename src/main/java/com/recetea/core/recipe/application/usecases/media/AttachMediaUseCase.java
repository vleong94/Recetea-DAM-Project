package com.recetea.core.recipe.application.usecases.media;

import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.user.domain.UserId;

public class AttachMediaUseCase implements IAttachMediaUseCase {

    private final IRecipeRepository recipeRepository;
    private final IMediaStorageService storageService;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public AttachMediaUseCase(IRecipeRepository recipeRepository,
                              IMediaStorageService storageService,
                              ITransactionManager transactionManager,
                              IUserSessionService sessionService) {
        this.recipeRepository = recipeRepository;
        this.storageService = storageService;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId, byte[] data, String originalName) {
        // Store first: if storage fails the DB is never touched.
        StorageResult stored = storageService.store(data, originalName);

        try {
            transactionManager.execute(() -> {
                Recipe recipe = recipeRepository.findById(recipeId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Receta no encontrada con ID: " + recipeId.value()));

                UserId currentUser = sessionService.getCurrentUserId()
                        .orElseThrow(AuthenticationRequiredException::new);
                if (!recipe.getAuthorId().equals(currentUser)) {
                    throw new UnauthorizedRecipeAccessException(
                            "El usuario " + currentUser.value() + " no tiene permiso para modificar esta receta.");
                }

                int sortOrder = recipe.getMediaItems().size();
                RecipeMedia media = new RecipeMedia(
                        null,
                        recipeId,
                        stored.storageKey(),
                        "LOCAL",
                        stored.mimeType(),
                        stored.sizeBytes(),
                        false,
                        sortOrder);

                recipe.addMedia(media);
                recipeRepository.update(recipe);
                return null;
            });
        } catch (RuntimeException e) {
            // DB failed: compensate by removing the already-stored file.
            storageService.delete(stored.storageKey());
            throw e;
        }
    }
}
