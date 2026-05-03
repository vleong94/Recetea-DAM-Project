package com.recetea.core.recipe.application.usecases.recipe;

import com.recetea.core.recipe.application.ports.in.dto.RecipeDetailResponse;
import com.recetea.core.recipe.application.ports.in.recipe.IGetRecipeByIdUseCase;
import com.recetea.core.recipe.application.ports.out.recipe.IRecipeRepository;
import com.recetea.core.recipe.domain.Recipe;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.user.application.ports.out.IUserRepository;
import com.recetea.core.user.domain.UserId;

import java.util.Optional;

/**
 * Reads the full recipe aggregate for the detail view — single
 * LATERAL-JSONB query at the JDBC layer + author-username + the
 * "already rated by current user" predicate.
 *
 * <p>The {@code alreadyRatedByCurrentUser} flag is computed by probing
 * {@code IRecipeRepository.hasUserRatedRecipe(uid, recipeId)} inside the
 * same call — the controller uses it to disable
 * {@code RatingComponent}. Anonymous sessions short-circuit to
 * {@code false} (no probe).
 *
 * <p>Author username is resolved via {@code IUserRepository.findById(...)}
 * — null when the account has been deleted, in which case the adapter
 * downstream renders a localised "Desconocido" fallback. The Recipe
 * domain stays free of UI display strings.
 *
 * <p><b>ES — </b>Lee el agregado completo de receta para la vista de
 * detalle — una única consulta LATERAL-JSONB en la capa JDBC + el
 * username del autor + el predicado "ya valorado por el usuario
 * actual".
 *
 * <p>El flag {@code alreadyRatedByCurrentUser} lo calcula sondeando
 * {@code IRecipeRepository.hasUserRatedRecipe(uid, recipeId)} dentro
 * de la misma llamada — el controlador lo usa para deshabilitar
 * {@code RatingComponent}. Las sesiones anónimas corto-circuitan a
 * {@code false} (sin sondeo).
 *
 * <p>El username del autor se resuelve vía
 * {@code IUserRepository.findById(...)} — null cuando la cuenta ha
 * sido eliminada, en cuyo caso el adaptador aguas abajo renderiza
 * un fallback localizado "Desconocido". El dominio Recipe se
 * mantiene libre de cadenas de presentación de UI.
 */
public class GetRecipeByIdUseCase implements IGetRecipeByIdUseCase {

    private final IRecipeRepository  repository;
    private final IUserRepository    userRepository;
    private final IUserSessionService sessionService;

    public GetRecipeByIdUseCase(IRecipeRepository repository,
                                IUserRepository userRepository,
                                IUserSessionService sessionService) {
        this.repository     = repository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @Override
    public Optional<RecipeDetailResponse> execute(RecipeId recipeId) {
        Optional<UserId> currentUser = sessionService.getCurrentUserId();
        return repository.findById(recipeId)
                .map(recipe -> mapToResponse(recipe, currentUser));
    }

    private RecipeDetailResponse mapToResponse(Recipe recipe, Optional<UserId> currentUser) {
        boolean alreadyRated = currentUser
                .map(uid -> repository.hasUserRatedRecipe(uid, recipe.getId()))
                .orElse(false);

        // Author username comes from the User aggregate; null if the account was deleted.
        // The UI surface shows a localised "Desconocido" fallback for that case.
        String authorUsername = userRepository.findById(recipe.getAuthorId())
                .map(u -> u.username().value())
                .orElse(null);

        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getAuthorId(),
                authorUsername,
                recipe.getCategory().id(),
                recipe.getCategory().name(),
                recipe.getDifficulty().id(),
                recipe.getDifficulty().name(),
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
