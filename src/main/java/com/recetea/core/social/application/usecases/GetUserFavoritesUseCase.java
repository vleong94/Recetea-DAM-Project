package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.AuthenticationRequiredException;
import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IGetUserFavoritesUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;
import com.recetea.core.user.domain.UserId;

import java.util.List;

/**
 * Returns the list of {@link RecipeId}s the current user has favourited —
 * <em>not</em> the hydrated recipe summaries. Hydration is the caller's
 * responsibility, typically via {@code GetRecipeSummariesByIdsUseCase}, so
 * the social bounded context stays free of recipe DTO knowledge.
 *
 * <p>{@code UserProfileController} chains the two use cases: this one feeds
 * the favourites tab a list of ids, then the summary-by-ids use case
 * resolves them into the cards the gallery renders.
 *
 * <p>Throws {@code AuthenticationRequiredException} for anonymous flows —
 * the favourites tab is only reachable from the workspace, which already
 * requires a session, so this is a defensive check.
 *
 * <p><b>ES — </b>Devuelve la lista de {@link RecipeId} que el usuario
 * actual ha favoritado — <em>no</em> los resúmenes de receta
 * hidratados. La hidratación es responsabilidad del llamador,
 * típicamente vía {@code GetRecipeSummariesByIdsUseCase}, para que el
 * contexto delimitado social quede libre de conocimiento sobre los
 * DTOs de receta.
 *
 * <p>{@code UserProfileController} encadena los dos casos de uso:
 * éste alimenta la pestaña de favoritos con una lista de ids, y luego
 * el caso de uso summary-by-ids los resuelve en las tarjetas que la
 * galería renderiza.
 *
 * <p>Lanza {@code AuthenticationRequiredException} para flujos
 * anónimos — la pestaña de favoritos sólo es accesible desde el
 * workspace, que ya requiere sesión, así que es una comprobación
 * defensiva.
 */
public class GetUserFavoritesUseCase implements IGetUserFavoritesUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final IUserSessionService sessionService;

    public GetUserFavoritesUseCase(IFavoriteRepository favoriteRepository,
                                   IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.sessionService     = sessionService;
    }

    @Override
    public List<RecipeId> execute() {
        UserId userId = sessionService.getCurrentUserId()
                .orElseThrow(AuthenticationRequiredException::new);
        return favoriteRepository.findFavoriteRecipeIdsByUserId(userId);
    }
}
