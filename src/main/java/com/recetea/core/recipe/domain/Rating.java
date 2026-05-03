package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.Score;
import com.recetea.core.user.domain.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One vote cast by a user on a recipe. Carries the score, an optional
 * comment, the timestamp, and a hydrated {@code username} for display.
 *
 * <p>{@code username} is populated only during DB hydration via the LATERAL
 * JOIN in {@code SELECT_FULL_AGGREGATE} (see
 * {@code RecipeMapper#mapRatingsJson}); it stays {@code null} when a
 * Rating is created in-memory through {@link Recipe#addRating}, since the
 * write path doesn't need it. The detail page reads {@code username}
 * directly to render the rating list — null collapses to a blank cell on
 * the rare case where the voter's account has been deleted between the
 * vote and the fetch.
 *
 * <p>Comment is required by the schema (column NOT NULL) but may be the
 * empty string; the 1000-char cap matches the DB column width.
 *
 * <p><b>ES — </b>Una valoración emitida por un usuario sobre una receta.
 * Lleva la puntuación, un comentario opcional, la marca temporal y un
 * {@code username} hidratado para presentación.
 *
 * <p>{@code username} sólo se rellena durante la hidratación desde la BD
 * mediante el LATERAL JOIN en {@code SELECT_FULL_AGGREGATE} (véase
 * {@code RecipeMapper#mapRatingsJson}); permanece {@code null} cuando un
 * Rating se crea en memoria a través de {@link Recipe#addRating}, ya que
 * la ruta de escritura no lo necesita. La página de detalle lee
 * {@code username} directamente para renderizar la lista de valoraciones —
 * null colapsa a una celda en blanco en el raro caso de que la cuenta del
 * votante haya sido eliminada entre el voto y el fetch.
 *
 * <p>El comentario es obligatorio por el esquema (columna NOT NULL) pero
 * puede ser cadena vacía; el límite de 1000 caracteres coincide con el
 * ancho de la columna de la BD.
 */
public record Rating(UserId userId, Score score, String comment, LocalDateTime createdAt, String username) {

    public Rating {
        Objects.requireNonNull(userId,    "userId is required.");
        Objects.requireNonNull(score,     "score is required.");
        Objects.requireNonNull(comment,   "comment is required.");
        if (comment.length() > 1000) {
            throw new IllegalArgumentException("Comment must not exceed 1000 characters.");
        }
        Objects.requireNonNull(createdAt, "createdAt is required.");
        // username intentionally allowed to be null (domain-creation path)
    }

    /** Domain-creation overload — username unknown at write time, filled in on subsequent DB hydration. */
    public Rating(UserId userId, Score score, String comment, LocalDateTime createdAt) {
        this(userId, score, comment, createdAt, null);
    }
}
