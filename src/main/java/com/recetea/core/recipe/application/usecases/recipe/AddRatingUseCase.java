package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;
import com.recetea.core.recipe.application.ports.in.recipe.IAddRatingUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.InvalidRecipeDataException;
import com.recetea.core.recipe.domain.RecipeNotFoundException;
import com.recetea.core.shared.application.ConcurrencyGuard;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.shared.application.ports.out.ITransactionManager;

/**
 * Records a single rating on a recipe — load the aggregate, append the
 * vote via {@code Recipe.addRating(...)} (which recomputes social metrics
 * and rejects self-rating + double-vote), then hand the new aggregate to
 * {@code IRecipeRepository.update}. The repository's smart-sync upserts
 * the rating row and updates {@code recipes.average_score} +
 * {@code recipes.total_ratings} in the same transaction.
 *
 * <p>{@link com.recetea.core.shared.application.ConcurrencyGuard} wraps
 * the call because high-traffic recipes can attract concurrent rating
 * submissions; bounding the parallel writes prevents pool exhaustion.
 *
 * <p>Self-rating and duplicate-vote are domain invariants enforced inside
 * {@code Recipe.addRating} — the use case body doesn't probe for them.
 * Structural input validation (score range, comment length) runs first
 * via {@code AddRatingRequest.validate()}.
 *
 * <p><b>ES — </b>Registra una única valoración sobre una receta —
 * carga el agregado, añade el voto vía {@code Recipe.addRating(...)}
 * (que recomputa las métricas sociales y rechaza la auto-valoración
 * y el voto duplicado), y luego entrega el agregado nuevo a
 * {@code IRecipeRepository.update}. El smart-sync del repositorio
 * hace upsert de la fila de valoración y actualiza
 * {@code recipes.average_score} + {@code recipes.total_ratings} en
 * la misma transacción.
 *
 * <p>{@link com.recetea.core.shared.application.ConcurrencyGuard}
 * envuelve la llamada porque las recetas con tráfico alto pueden
 * atraer envíos de valoración concurrentes; acotar las escrituras
 * paralelas evita el agotamiento del pool.
 *
 * <p>La auto-valoración y el voto duplicado son invariantes de
 * dominio aplicadas dentro de {@code Recipe.addRating} — el cuerpo
 * del caso de uso no los sondea. La validación estructural de
 * entrada (rango de puntuación, longitud del comentario) corre
 * primero vía {@code AddRatingRequest.validate()}.
 */
public class AddRatingUseCase implements IAddRatingUseCase {

    private final IRecipeRepository recipeRepository;
    private final ITransactionManager transactionManager;
    private final IUserSessionService sessionService;
    private final ConcurrencyGuard concurrencyGuard;

    public AddRatingUseCase(IRecipeRepository recipeRepository,
                            ITransactionManager transactionManager,
                            IUserSessionService sessionService,
                            ConcurrencyGuard concurrencyGuard) {
        this.recipeRepository = recipeRepository;
        this.transactionManager = transactionManager;
        this.sessionService = sessionService;
        this.concurrencyGuard = concurrencyGuard;
    }

    @Override
    public void execute(AddRatingRequest request) {
        // Non-short-circuit input validation; throws with the full accumulated error list.
        request.validate().getOrThrow(InvalidRecipeDataException::new);

        concurrencyGuard.run(() -> transactionManager.execute(() -> {
            var recipe = recipeRepository.findById(request.recipeId())
                    .orElseThrow(() -> new RecipeNotFoundException(request.recipeId().value()));
            var voterId = sessionService.getCurrentUserId()
                    .orElseThrow(AuthenticationRequiredException::new);
            // addRating returns the post-rating aggregate (Recipe is now an immutable record).
            recipeRepository.update(recipe.addRating(voterId, request.score(), request.comment()));
            return null;
        }));
    }
}
