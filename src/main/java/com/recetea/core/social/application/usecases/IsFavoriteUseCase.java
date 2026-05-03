package com.recetea.core.social.application.usecases;

import com.recetea.core.recipe.domain.vo.RecipeId;
import com.recetea.core.shared.application.ports.in.IUserSessionService;
import com.recetea.core.social.application.ports.in.IIsFavoriteUseCase;
import com.recetea.core.social.application.ports.out.IFavoriteRepository;

/**
 * Single-recipe predicate: is the recipe in the current user's favourites?
 * Returns {@code false} (not an exception) when no session is active — the
 * UI uses this to seed each card's heart-icon state on render, and an
 * anonymous user has no favourites by definition.
 *
 * <p>Read-only, so no transaction wrapping is needed. The repository's
 * {@code SELECT 1 FROM favorites WHERE …} probe runs through
 * {@code BaseJdbcRepository.withConnection}, which acquires a fresh
 * connection from HikariCP and returns it via try-with-resources.
 *
 * <p><b>ES — </b>Predicado de una sola receta: ¿está la receta en los
 * favoritos del usuario actual? Devuelve {@code false} (no una
 * excepción) cuando no hay sesión activa — la UI usa esto para
 * sembrar el estado del icono de corazón de cada tarjeta al
 * renderizar, y un usuario anónimo no tiene favoritos por definición.
 *
 * <p>Sólo lectura, así que no hace falta envolverlo en transacción. El
 * sondeo {@code SELECT 1 FROM favorites WHERE …} del repositorio se
 * ejecuta a través de {@code BaseJdbcRepository.withConnection}, que
 * adquiere una conexión nueva de HikariCP y la devuelve vía
 * try-with-resources.
 */
public class IsFavoriteUseCase implements IIsFavoriteUseCase {

    private final IFavoriteRepository favoriteRepository;
    private final IUserSessionService sessionService;

    public IsFavoriteUseCase(IFavoriteRepository favoriteRepository,
                             IUserSessionService sessionService) {
        this.favoriteRepository = favoriteRepository;
        this.sessionService = sessionService;
    }

    @Override
    public boolean execute(RecipeId recipeId) {
        return sessionService.getCurrentUserId()
                .map(userId -> favoriteRepository.isFavorite(userId, recipeId))
                .orElse(false);
    }
}
