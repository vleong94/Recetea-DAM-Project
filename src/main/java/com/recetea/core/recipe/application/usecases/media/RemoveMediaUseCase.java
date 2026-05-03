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

/**
 * Removes a media item from a recipe — DB delete first, file delete
 * second. The inverse ordering of {@code AttachMediaUseCase}: the DB is
 * the authority on whether the file is referenced, so removing the row
 * first means a mid-flight failure leaves an orphan file (cheap to clean
 * up) rather than a row pointing at missing bytes (corrupt state).
 *
 * <p>The storage key needed for the file delete is captured from inside
 * the transactional block and used after commit — the
 * {@code transactionManager.execute} return value carries it across the
 * boundary so the file-delete call doesn't need to re-load the recipe.
 *
 * <p><b>ES — </b>Elimina un elemento multimedia de una receta —
 * primero el delete de BD, después el del archivo. El orden inverso
 * al de {@code AttachMediaUseCase}: la BD es la autoridad sobre si
 * el archivo está referenciado, así que eliminar la fila primero
 * implica que un fallo a mitad de operación deja un archivo
 * huérfano (barato de limpiar) en lugar de una fila apuntando a
 * bytes inexistentes (estado corrupto).
 *
 * <p>La storage key necesaria para borrar el archivo se captura
 * dentro del bloque transaccional y se usa tras el commit — el
 * valor de retorno de {@code transactionManager.execute} la lleva a
 * través de la frontera para que la llamada de borrado de archivo
 * no necesite volver a cargar la receta.
 */
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
                            "Recipe not found with ID: " + recipeId.value()));

            UserId currentUser = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            if (!recipe.getAuthorId().equals(currentUser)) {
                throw new UnauthorizedRecipeAccessException(
                        "User " + currentUser.value() + " is not authorized to modify this recipe.");
            }

            String key = recipe.getMediaItems().stream()
                    .filter(m -> mediaId.equals(m.id()))
                    .map(RecipeMedia::storageKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Media item not found with ID: " + mediaId.value()));

            recipeRepository.update(recipe.removeMedia(mediaId));
            return key;
        });

        storageService.delete(storageKey);
    }
}
