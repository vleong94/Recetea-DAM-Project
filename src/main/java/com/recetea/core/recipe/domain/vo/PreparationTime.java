package com.recetea.core.recipe.domain.vo;

/**
 * Recipe preparation time in minutes. Bounded between {@code 1} and
 * {@link #MAX_MINUTES} (= 30 days = {@code 30 * 24 * 60}). The upper bound
 * exists to reject obvious data-entry mistakes (a typo turning 30 into 30000)
 * and to keep the database column ({@code prep_time_min INTEGER}) within
 * sane range even though the SQL CHECK alone wouldn't catch a typo within the
 * INTEGER limit.
 *
 * <p>Compact-constructor invariants: {@code 1 <= value <= MAX_MINUTES}. Zero
 * is rejected because a recipe with no preparation time is meaningless;
 * negatives are rejected because they're nonsense.
 *
 * <p><b>ES — </b>Tiempo de preparación de la receta en minutos. Acotado entre
 * {@code 1} y {@link #MAX_MINUTES} (= 30 días = {@code 30 * 24 * 60}). El
 * límite superior existe para rechazar errores obvios de captura de datos (un
 * tipeo que convierte 30 en 30000) y para mantener la columna de la base de
 * datos ({@code prep_time_min INTEGER}) dentro de un rango razonable, ya que
 * el CHECK de SQL por sí solo no detectaría un tipeo dentro del límite del
 * INTEGER.
 *
 * <p>Invariantes del constructor compacto: {@code 1 <= value <= MAX_MINUTES}.
 * El cero se rechaza porque una receta sin tiempo de preparación carece de
 * sentido; los negativos se rechazan porque son absurdos.
 */
public record PreparationTime(int value) {
    public static final int MAX_MINUTES = 43200; // 30 days

    public PreparationTime {
        if (value <= 0)
            throw new IllegalArgumentException("Preparation time must be greater than zero.");
        if (value > MAX_MINUTES)
            throw new IllegalArgumentException(
                    "Preparation time cannot exceed " + MAX_MINUTES + " minutes (30 days).");
    }
}
