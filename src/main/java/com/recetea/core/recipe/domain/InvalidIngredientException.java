package com.recetea.core.recipe.domain;

/**
 * Thrown by the {@code RecipeIngredient} compact constructor when a single
 * ingredient line is structurally invalid (null id, non-positive quantity,
 * etc.). Distinct from {@link InvalidRecipeDataException} which aggregates
 * recipe-wide validation across all child collections.
 *
 * <p>Extends {@link RuntimeException} rather than {@code DomainException}
 * because it indicates a programming error inside the persistence
 * round-trip — the persisted data should already be valid by the time it
 * reaches the domain. User input never reaches this class directly;
 * {@code SaveRecipeRequest.validate()} catches structural problems first
 * and surfaces them through {@code InvalidRecipeDataException}.
 *
 * <p><b>ES — </b>La lanza el constructor compacto de
 * {@code RecipeIngredient} cuando una única línea de ingrediente es
 * estructuralmente inválida (id nulo, cantidad no positiva, etc.).
 * Diferente de {@link InvalidRecipeDataException}, que agrega la
 * validación a nivel de receta entera entre todas las colecciones hijas.
 *
 * <p>Hereda de {@link RuntimeException} y no de {@code DomainException}
 * porque indica un error de programación dentro del round-trip de
 * persistencia — los datos persistidos ya deberían ser válidos cuando
 * llegan al dominio. La entrada del usuario no alcanza esta clase
 * directamente; {@code SaveRecipeRequest.validate()} captura los
 * problemas estructurales primero y los expone a través de
 * {@code InvalidRecipeDataException}.
 */
public class InvalidIngredientException extends RuntimeException {

    public InvalidIngredientException(String message) {
        super(message);
    }
}
