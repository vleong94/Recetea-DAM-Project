package com.recetea.core.user.domain.vo;

/**
 * User-facing handle used for login and display. Validation lives at the
 * value-object boundary so the {@code RegisterUserUseCase} call-path and the
 * {@code UserMapper} hydration path both produce a {@code Username} that
 * downstream consumers can trust without re-checking.
 *
 * <p>Compact-constructor invariants: not null, not blank, and at least
 * {@link #MIN_LENGTH} characters after trimming. The lower bound rules out
 * one- and two-character handles ("a", "ab") that would create UX friction
 * around uniqueness collisions and potentially be hard to address in UI mentions.
 *
 * <p><b>ES — </b>Identificador de usuario expuesto, usado para el login y la
 * presentación. La validación vive en la frontera del value object, de modo
 * que tanto el camino de {@code RegisterUserUseCase} como la hidratación de
 * {@code UserMapper} producen un {@code Username} en el que los consumidores
 * descendentes pueden confiar sin volver a comprobarlo.
 *
 * <p>Invariantes del constructor compacto: no nulo, no en blanco, y al menos
 * {@link #MIN_LENGTH} caracteres tras hacer trim. El límite inferior descarta
 * handles de uno o dos caracteres ("a", "ab") que generarían fricción UX por
 * colisiones de unicidad y serían difíciles de manejar en menciones en la UI.
 */
public record Username(String value) {
    public static final int MIN_LENGTH = 3;

    public Username {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (value.trim().length() < MIN_LENGTH)
            throw new IllegalArgumentException("Username must be at least " + MIN_LENGTH + " characters long.");
    }
}
