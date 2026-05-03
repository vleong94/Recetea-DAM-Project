package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Predicate used by the recipe detail page to set the initial state of the
 * favorite-toggle button before the user interacts with it.
 *
 * <p>Returns {@code false} for anonymous sessions (no authentication
 * exception) so the detail page renders cleanly for logged-out users while
 * keeping the toggle disabled at the controller layer.
 *
 * <p><b>ES — </b>Predicado usado por la página de detalle de receta para
 * establecer el estado inicial del botón de favorito antes de que el
 * usuario interactúe con él.
 *
 * <p>Devuelve {@code false} para sesiones anónimas (sin excepción de
 * autenticación) para que la página de detalle renderice limpiamente
 * para usuarios deslogueados, manteniendo el toggle deshabilitado en la
 * capa del controlador.
 */
public interface IIsFavoriteUseCase {
    boolean execute(RecipeId recipeId);
}
