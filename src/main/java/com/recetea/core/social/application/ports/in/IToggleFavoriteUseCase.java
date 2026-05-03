package com.recetea.core.social.application.ports.in;

import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Idempotent flip of the (user, recipe) row in the {@code favorites}
 * association table — present → delete, absent → insert.
 *
 * <p>Wrapped in a single transaction so a concurrent click cannot create a
 * duplicate row; the unique index on {@code (user_id, recipe_id)} is the
 * authoritative guard. Throws {@code AuthenticationRequiredException}
 * when no session is bound.
 *
 * <p>Self-favoriting is allowed by design — users routinely save their own
 * recipes for quick access from the favorites tab.
 *
 * <p><b>ES — </b>Toggle idempotente de la fila (user, recipe) en la tabla
 * de asociación {@code favorites} — presente → delete, ausente → insert.
 *
 * <p>Envuelto en una sola transacción para que un click concurrente no
 * pueda crear una fila duplicada; el índice único sobre
 * {@code (user_id, recipe_id)} es la garantía autoritativa. Lanza
 * {@code AuthenticationRequiredException} cuando no hay sesión ligada.
 *
 * <p>Auto-favoritarse está permitido por diseño — los usuarios guardan
 * habitualmente sus propias recetas para acceso rápido desde la pestaña
 * de favoritos.
 */
public interface IToggleFavoriteUseCase {
    void execute(RecipeId recipeId);
}
