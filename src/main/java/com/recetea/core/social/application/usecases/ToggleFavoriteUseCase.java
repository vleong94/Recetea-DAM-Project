package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;
import com.recetea.core.social.application.ports.in.IToggleFavoriteUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;

/**
 * Pin/unpin a recipe to/from the current user's favourites. The implementation
 * inverts whatever state the {@code (user_id, recipe_id)} pair is currently in:
 * if a row exists, delete it; if not, insert it. Wrapped in
 * {@code transactionManager.execute(...)} so the read-and-write pair commits
 * atomically — without the transaction, a concurrent click on the same recipe
 * could insert a duplicate before the read completes.
 *
 * <p>Throws {@code AuthenticationRequiredException} when called from an
 * anonymous flow; the dashboard's heart icon is hidden in that case but a
 * deep-link or direct API consumer could still hit the use case unauthenticated.
 *
 * <p><b>ES — </b>Marca/desmarca una receta en los favoritos del usuario
 * actual. La implementación invierte el estado actual del par
 * {@code (user_id, recipe_id)}: si la fila existe, la borra; si no,
 * la inserta. Envuelto en {@code transactionManager.execute(...)} para
 * que el par lectura-escritura haga commit atómicamente — sin la
 * transacción, un click concurrente sobre la misma receta podría
 * insertar un duplicado antes de que la lectura termine.
 *
 * <p>Lanza {@code AuthenticationRequiredException} cuando se llama
 * desde un flujo anónimo; el icono de corazón del dashboard se oculta
 * en ese caso, pero un deep-link o un consumidor directo de API
 * podrían llegar al caso de uso sin autenticar.
 */
public class ToggleFavoriteUseCase implements IToggleFavoriteUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;

    public ToggleFavoriteUseCase(IFavoriteRepository favoriteRepository,
                                 ITransactionManager transactionManager,
                                 IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
    }

    @Override
    public void execute(RecipeId recipeId) {
        UserId userId = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);
        transactionManager.execute(() -> {
            if (favoriteRepository.isFavorite(userId, recipeId)) {
                favoriteRepository.delete(userId, recipeId);
            } else {
                favoriteRepository.save(userId, recipeId);
            }
        });
    }
}
