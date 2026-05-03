package com.recetea.core.recipe.domain.vo;

/**
 * Strongly-typed identifier for a {@code Difficulty} master-data row.
 * The {@code difficulties} table is treated as an enum-shaped catalogue
 * (4 fixed rows: Easy / Medium / Hard / Very Hard), so this wrapper exists
 * primarily for compile-time separation from other id shapes — not to police
 * an open catalogue.
 *
 * <p>Compact-constructor invariant: positive value only.
 *
 * <p><b>ES — </b>Identificador fuertemente tipado para una fila de los datos
 * maestros {@code Difficulty}. La tabla {@code difficulties} se trata como un
 * catálogo con forma de enum (4 filas fijas: Fácil / Media / Difícil / Muy
 * difícil), por lo que este wrapper existe principalmente para la separación
 * en tiempo de compilación frente a otros tipos de id, no para vigilar un
 * catálogo abierto.
 *
 * <p>Invariante del constructor compacto: solo valores positivos.
 */
public record DifficultyId(int value) {
    public DifficultyId {
        if (value <= 0) throw new IllegalArgumentException("DifficultyId must be positive.");
    }
}
