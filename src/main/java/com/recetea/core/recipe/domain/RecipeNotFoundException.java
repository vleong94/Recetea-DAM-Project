package com.recetea.core.recipe.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;

/**
 * Thrown when a use case targets a recipe id that has no matching row.
 * Most commonly raised after a deletion races with a navigation click —
 * the user clicks "view" on a card whose row is concurrently deleted.
 *
 * <p>Maps to {@link ErrorCode#NOT_FOUND} → WARNING-level dialog. The id
 * is interpolated as an int because callers always have the underlying
 * primitive available (avoids an extra {@code RecipeId.value()} call at
 * the throw site).
 *
 * <p><b>ES — </b>Se lanza cuando un caso de uso apunta a un id de receta
 * sin fila correspondiente. Lo habitual es que se produzca cuando una
 * eliminación entra en carrera con un click de navegación — el usuario
 * pincha "ver" en una tarjeta cuya fila se está eliminando
 * concurrentemente.
 *
 * <p>Mapea a {@link ErrorCode#NOT_FOUND} → diálogo de nivel WARNING. El id
 * se interpola como int porque los llamadores siempre tienen disponible
 * el primitivo subyacente (evita una llamada extra a
 * {@code RecipeId.value()} en el sitio del throw).
 */
public class RecipeNotFoundException extends DomainException {

    public RecipeNotFoundException(int id) {
        super(ErrorCode.NOT_FOUND, "Recipe not found with ID: " + id);
    }
}
