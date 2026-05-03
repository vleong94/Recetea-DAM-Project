package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.SaveRecipeRequest;
import com.recetea.core.recipe.domain.vo.RecipeId;

/**
 * Persists a new recipe authored by the currently logged-in user. The session
 * user is sourced from {@code IUserSessionService} inside the implementation —
 * the request DTO never carries an authorId, preventing identity-spoofing
 * via a crafted payload.
 *
 * <p>Throws {@code AuthenticationRequiredException} when no session is active,
 * and {@code InvalidRecipeDataException} carrying the accumulated validation
 * errors when {@code SaveRecipeRequest.validate()} fails. Returns the newly
 * generated {@link RecipeId} on success.
 *
 * <p><b>ES — </b>Persiste una nueva receta cuyo autor es el usuario
 * actualmente logueado. El usuario de sesión se obtiene de
 * {@code IUserSessionService} dentro de la implementación — el DTO de
 * request nunca lleva un authorId, lo que evita la suplantación de
 * identidad mediante un payload manipulado.
 *
 * <p>Lanza {@code AuthenticationRequiredException} cuando no hay sesión
 * activa, e {@code InvalidRecipeDataException} con los errores de
 * validación acumulados cuando {@code SaveRecipeRequest.validate()}
 * falla. Devuelve el {@link RecipeId} recién generado en caso de éxito.
 */
public interface ICreateRecipeUseCase {

    RecipeId execute(SaveRecipeRequest request);
}
