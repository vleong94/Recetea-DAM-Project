package com.recetea.core.recipe.domain.vo;

/**
 * Number of servings a recipe yields. Bounded between {@code 1} and
 * {@link #MAX_SERVINGS}. The upper cap is a defensive limit against
 * data-entry typos rather than a domain rule — no real recipe targets
 * 1 000+ servings, but a UI numeric field might accept the value.
 *
 * <p>Compact-constructor invariants: {@code 1 <= value <= MAX_SERVINGS}.
 *
 * <p><b>ES — </b>Número de raciones que produce una receta. Acotado entre
 * {@code 1} y {@link #MAX_SERVINGS}. El límite superior es una protección
 * defensiva frente a tipeos en la captura de datos, no una regla de dominio:
 * ninguna receta real apunta a 1 000+ raciones, pero un campo numérico de la
 * UI podría aceptar el valor.
 *
 * <p>Invariantes del constructor compacto:
 * {@code 1 <= value <= MAX_SERVINGS}.
 */
public record Servings(int value) {
    public static final int MAX_SERVINGS = 1000;

    public Servings {
        if (value <= 0)
            throw new IllegalArgumentException("Servings must be greater than zero.");
        if (value > MAX_SERVINGS)
            throw new IllegalArgumentException(
                    "Servings cannot exceed " + MAX_SERVINGS + ".");
    }
}
