package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.UnitId;

/**
 * Catalogue entity — one row in {@code units}. Two display strings:
 * {@code name} ("Gramo") and {@code abbreviation} ("g"). The
 * {@link #toString()} concatenates both ("Gramo (g)") for compact dropdown
 * rendering — distinct from {@link Category}/{@link Difficulty}/{@link Ingredient}
 * whose toString returns the bare name only.
 *
 * <p><b>ES — </b>Entidad de catálogo — una fila en {@code units}. Dos
 * cadenas de presentación: {@code name} ("Gramo") y {@code abbreviation}
 * ("g"). El {@link #toString()} concatena ambas ("Gramo (g)") para
 * renderizado compacto en desplegables — a diferencia de
 * {@link Category}/{@link Difficulty}/{@link Ingredient}, cuyo toString
 * devuelve únicamente el nombre escueto.
 */
public record Unit(UnitId id, String name, String abbreviation) {

    public Unit {
        if (name == null || name.isBlank()) {
            throw new UnitValidationException("Unit name is required.");
        }
        if (abbreviation == null || abbreviation.isBlank()) {
            throw new UnitValidationException("Unit abbreviation is required.");
        }
        name = name.trim();
        abbreviation = abbreviation.trim();
    }

    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }

    public static class UnitValidationException extends RuntimeException {
        public UnitValidationException(String message) {
            super(message);
        }
    }
}
