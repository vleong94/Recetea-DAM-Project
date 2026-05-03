package com.recetea.core.recipe.domain.vo;

/**
 * Strongly-typed identifier for a {@code recipe_media} row (a single image
 * attached to a recipe). Note that {@code RecipeMedia} carries a nullable
 * {@code RecipeMediaId} — the field is {@code null} between in-memory creation
 * and the {@code INSERT … RETURNING} that populates it. {@code Recipe.addMedia}
 * accepts a media item with {@code id == null}; the partial-unique-index dance
 * in {@code RecipeMediaTableGateway.syncMedia} differentiates new vs. existing
 * rows by inspecting that null.
 *
 * <p>Compact-constructor invariant: positive value only — once the ID exists,
 * it must be a real database key.
 *
 * <p><b>ES — </b>Identificador fuertemente tipado para una fila de
 * {@code recipe_media} (una imagen asociada a una receta). Nótese que
 * {@code RecipeMedia} lleva un {@code RecipeMediaId} anulable — el campo es
 * {@code null} entre la creación en memoria y el {@code INSERT … RETURNING}
 * que lo rellena. {@code Recipe.addMedia} acepta un elemento multimedia con
 * {@code id == null}; el baile del índice único parcial en
 * {@code RecipeMediaTableGateway.syncMedia} diferencia filas nuevas de
 * existentes inspeccionando ese null.
 *
 * <p>Invariante del constructor compacto: solo valores positivos — una vez
 * existe el ID, debe ser una clave real de la base de datos.
 */
public record RecipeMediaId(int value) {
    public RecipeMediaId {
        if (value <= 0) throw new IllegalArgumentException("RecipeMediaId must be positive.");
    }
}
