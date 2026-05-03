package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.domain.vo.RecipeId;

import java.util.List;

/**
 * Returns the {@link RecipeId}s the current user has favorited, ordered by
 * favorite-creation timestamp descending (most recently favorited first).
 *
 * <p>The current user is resolved from {@code IUserSessionService} inside
 * the implementation — there is no parameter, since exposing it would let
 * a caller probe other users' favorites. Throws
 * {@code AuthenticationRequiredException} when no session exists.
 *
 * <p>The favorites tab feeds the resulting id list into
 * {@code IGetRecipeSummariesByIdsUseCase} and reorders the result to match,
 * preserving the "most recent" semantics.
 *
 * <p><b>ES — </b>Devuelve los {@link RecipeId} que el usuario actual ha
 * marcado como favoritos, ordenados por la marca temporal de creación
 * del favorito en orden descendente (los más recientes primero).
 *
 * <p>El usuario actual se resuelve desde {@code IUserSessionService}
 * dentro de la implementación — no hay parámetro, ya que exponerlo
 * permitiría a un llamador sondear los favoritos de otros usuarios.
 * Lanza {@code AuthenticationRequiredException} cuando no hay sesión.
 *
 * <p>La pestaña de favoritos pasa la lista de ids resultante a
 * {@code IGetRecipeSummariesByIdsUseCase} y reordena el resultado para
 * que cuadre, preservando la semántica de "más reciente".
 */
public interface IGetUserFavoritesUseCase {
    List<RecipeId> execute();
}
