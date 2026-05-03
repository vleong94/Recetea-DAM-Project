package com.recetea.core.recipe.application.usecases.media;

import com.recetea.core.recipe.application.ports.in.media.IAttachMediaUseCase;
import com.recetea.core.recipe.application.ports.out.media.IMediaStorageService;
import com.recetea.core.recipe.application.ports.out.media.StorageResult;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.RecipeMedia;
import com.recetea.core.recipe.domain.UnauthorizedRecipeAccessException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.shared.domain.Validation;
import com.recetea.core.user.domain.UserId;

import java.io.InputStream;
import java.util.List;

/**
 * Persists a media file against a recipe — the implementation pairs a
 * non-transactional storage write with a transactional DB update, then
 * compensates on DB failure.
 *
 * <p><b>Why the two-phase shape.</b> The storage call must run outside
 * the transaction because the file write is not part of the JDBC
 * connection's commit/rollback envelope; if the DB write later fails,
 * we still need to delete the orphan file. Catching the {@code RuntimeException}
 * around the transactional block and calling
 * {@code storageService.delete(stored.storageKey())} is the compensation —
 * the inverse of {@code RemoveMediaUseCase}'s ordering.
 *
 * <p>Ownership is verified inside the transaction so non-owners never
 * trigger a storage write. Validation is inline (rather than DTO-based)
 * because {@code IAttachMediaUseCase.execute} takes three positional
 * params — there is no request DTO at this boundary.
 *
 * <p><b>ES — </b>Persiste un archivo multimedia asociado a una receta
 * — la implementación combina una escritura de storage no
 * transaccional con un update transaccional de BD, y compensa ante
 * fallo de BD.
 *
 * <p><b>Por qué el shape de dos fases.</b> La llamada de storage
 * debe ejecutarse fuera de la transacción porque la escritura de
 * archivo no es parte del envoltorio de commit/rollback de la
 * conexión JDBC; si la escritura en BD falla después, aún
 * necesitamos borrar el archivo huérfano. Capturar la
 * {@code RuntimeException} alrededor del bloque transaccional y
 * llamar a {@code storageService.delete(stored.storageKey())} es la
 * compensación — el orden inverso del de
 * {@code RemoveMediaUseCase}.
 *
 * <p>La propiedad se verifica dentro de la transacción para que los
 * no-propietarios nunca disparen una escritura de storage. La
 * validación es inline (no basada en DTO) porque
 * {@code IAttachMediaUseCase.execute} recibe tres parámetros
 * posicionales — no hay DTO de request en esta frontera.
 */
public class AttachMediaUseCase implements IAttachMediaUseCase {

    private final IRecipeRepository recipeRepository;
    private final IMediaStorageService storageService;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final ConcurrencyGuard concurrencyGuard;

    public AttachMediaUseCase(IRecipeRepository recipeRepository,
                              IMediaStorageService storageService,
                              ITransactionManager transactionManager,
                              IUserSessionService sessionService,
                              ConcurrencyGuard concurrencyGuard) {
        this.recipeRepository = recipeRepository;
        this.storageService = storageService;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
        this.concurrencyGuard = concurrencyGuard;
    }

    @Override
    public void execute(RecipeId recipeId, InputStream data, String originalName) {
        // Non-short-circuit input validation. Inline rather than DTO-based because
        // IAttachMediaUseCase.execute takes three positional params (no request DTO).
        Validation.combine(
                Validation.validate(recipeId != null,
                        "Recipe ID is required."),
                Validation.validate(data != null,
                        "Image data is required."),
                Validation.validate(originalName != null && !originalName.isBlank(),
                        "Image filename is required.")
        ).getOrThrow(InvalidRecipeDataException::new);

        // Store first: if storage fails the DB is never touched.
        StorageResult stored = storageService.store(data, originalName);

        try {
            concurrencyGuard.run(() -> transactionManager.execute(() -> {
                Recipe recipe = recipeRepository.findById(recipeId)
                        .orElseThrow(() -> new InvalidRecipeDataException(
                                List.of("Recipe not found with ID: " + recipeId.value())));

                UserId currentUser = sessionService.getCurrentUserId()
                        .orElseThrow(AuthenticationRequiredException::new);
                if (!recipe.getAuthorId().equals(currentUser)) {
                    throw new UnauthorizedRecipeAccessException(
                            "User " + currentUser.value() + " is not authorized to modify this recipe.");
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

                recipeRepository.update(recipe.addMedia(media));
                return null;
            }));
        } catch (RuntimeException e) {
            // DB failed: compensate by removing the already-stored file.
            storageService.delete(stored.storageKey());
            throw e;
        }
    }
}
