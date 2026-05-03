package com.recetea.core.recipe.application.ports.in.recipe;

import com.recetea.core.recipe.application.ports.in.dto.AddRatingRequest;

/**
 * Adds a rating from the current session user to a recipe. The aggregate
 * enforces two domain rules at the {@code Recipe.addRating} call site:
 * authors cannot rate their own recipes, and a single user cannot rate the
 * same recipe twice. Both rejections surface as
 * {@code Recipe.RecipeValidationException} (which extends {@code DomainException}
 * with code {@code VALIDATION_ERROR}).
 *
 * <p>The implementation wraps the aggregate mutation + persistence in
 * {@code ConcurrencyGuard.run(...)} to throttle simultaneous rating-write
 * pressure against the HikariCP pool — high-traffic recipes can attract
 * concurrent rating attempts during release windows.
 *
 * <p><b>ES — </b>Añade una valoración del usuario actual de sesión a una
 * receta. El agregado aplica dos reglas de dominio en la llamada a
 * {@code Recipe.addRating}: los autores no pueden valorar sus propias
 * recetas, y un mismo usuario no puede valorar la misma receta dos veces.
 * Ambos rechazos se manifiestan como
 * {@code Recipe.RecipeValidationException} (que hereda de
 * {@code DomainException} con código {@code VALIDATION_ERROR}).
 *
 * <p>La implementación envuelve la mutación del agregado + persistencia
 * en {@code ConcurrencyGuard.run(...)} para acotar la presión simultánea
 * de escrituras de valoración sobre el pool de HikariCP — las recetas
 * con tráfico alto pueden atraer intentos concurrentes durante ventanas
 * de release.
 */
public interface IAddRatingUseCase {

    void execute(AddRatingRequest request);
}
