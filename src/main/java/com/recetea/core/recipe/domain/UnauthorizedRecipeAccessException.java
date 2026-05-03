package com.recetea.core.recipe.domain;

import com.recetea.core.shared.domain.DomainException;
import com.recetea.core.shared.domain.ErrorCode;

/**
 * Thrown when a session user attempts to mutate a recipe they don't own
 * (update, delete, attach/remove media). Ownership is enforced inside the
 * use case by comparing {@code Recipe.authorId()} against the session user
 * — not at the SQL layer, since reads remain public.
 *
 * <p>Maps to {@link ErrorCode#UNAUTHORIZED} → WARNING-level dialog. The
 * message is left to the caller because the throw sites differ enough
 * (delete vs. update vs. media) that a single canned string would lose
 * precision in logs.
 *
 * <p><b>ES — </b>Se lanza cuando un usuario en sesión intenta mutar una
 * receta que no le pertenece (actualizar, eliminar, adjuntar/quitar
 * multimedia). La propiedad se aplica dentro del caso de uso comparando
 * {@code Recipe.authorId()} con el usuario de sesión — no en la capa SQL,
 * ya que las lecturas siguen siendo públicas.
 *
 * <p>Mapea a {@link ErrorCode#UNAUTHORIZED} → diálogo de nivel WARNING. El
 * mensaje se delega al llamador porque los sitios de throw difieren lo
 * suficiente (delete vs. update vs. multimedia) como para que una cadena
 * estándar perdiera precisión en los logs.
 */
public class UnauthorizedRecipeAccessException extends DomainException {

    public UnauthorizedRecipeAccessException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
