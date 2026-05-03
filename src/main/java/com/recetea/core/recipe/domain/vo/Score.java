package com.recetea.core.recipe.domain.vo;

/**
 * Rating score, bounded to the 1–5 inclusive star scale used by the social
 * rating engine. The bound is enforced at the value-object boundary so every
 * downstream consumer ({@code Rating}, {@code Recipe.addRating}, the
 * {@code ratings.score} CHECK constraint mirroring this same range) can trust
 * that a {@code Score} reaching it is already valid.
 *
 * <p>Compact-constructor invariant: {@code 1 <= value <= 5}. Out-of-range input
 * fails fast with an {@link IllegalArgumentException} — the UI's star strip
 * never produces an out-of-range value, so an exception here indicates a
 * programming mistake (e.g. an XML import payload bypassing the form).
 *
 * <p><b>ES — </b>Puntuación de valoración, acotada a la escala inclusiva 1–5
 * de estrellas que usa el motor social de valoraciones. El acotamiento se
 * aplica en la frontera del value object, de modo que cada consumidor
 * descendente ({@code Rating}, {@code Recipe.addRating}, la restricción CHECK
 * {@code ratings.score} que refleja el mismo rango) puede confiar en que un
 * {@code Score} que le llega ya es válido.
 *
 * <p>Invariante del constructor compacto: {@code 1 <= value <= 5}. La entrada
 * fuera de rango falla rápido con {@link IllegalArgumentException} — la tira
 * de estrellas de la UI nunca produce un valor fuera de rango, así que una
 * excepción aquí indica un error de programación (por ejemplo, un payload de
 * importación XML que se salta el formulario).
 */
public record Score(int value) {
    public Score {
        if (value < 1 || value > 5)
            throw new IllegalArgumentException("Score must be between 1 and 5.");
    }
}
