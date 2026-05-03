package com.recetea.core.recipe.domain.vo;

/**
 * Strongly-typed identifier for a {@code unit_measures} catalogue row.
 * Type-safety wrapper that keeps method signatures self-documenting and
 * prevents the kind of ID swap that's easy to make when every identifier
 * is just {@code int}.
 *
 * <p>Compact-constructor invariant: positive value only.
 *
 * <p><b>ES — </b>Identificador fuertemente tipado para una fila del catálogo
 * {@code unit_measures}. Wrapper de seguridad de tipos que mantiene las firmas
 * de método autoexplicativas y evita el típico intercambio de IDs fácil de
 * cometer cuando todos los identificadores son simplemente {@code int}.
 *
 * <p>Invariante del constructor compacto: solo valores positivos.
 */
public record UnitId(int value) {
    public UnitId {
        if (value <= 0) throw new IllegalArgumentException("UnitId must be positive.");
    }
}
