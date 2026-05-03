package com.recetea.core.recipe.domain.vo;

/**
 * Strongly-typed identifier for a {@code Category} master-data row. Type wrapper
 * around the database surrogate key — compiler-enforced separation from
 * {@code DifficultyId}, {@code IngredientId}, and other adjacent identifiers
 * that share the same underlying primitive shape.
 *
 * <p>Compact-constructor invariant: positive value only. The first six category
 * IDs are frozen by convention (see {@code 02_data_seeding.sql}'s header comment)
 * so existing recipes continue to resolve their {@code category_id} foreign key
 * unchanged across master-data expansions.
 *
 * <p><b>ES — </b>Identificador fuertemente tipado para una fila de los datos
 * maestros {@code Category}. Wrapper de tipo sobre la clave subrogada de la
 * base de datos — separación garantizada por el compilador frente a
 * {@code DifficultyId}, {@code IngredientId} y otros identificadores adyacentes
 * que comparten la misma forma primitiva subyacente.
 *
 * <p>Invariante del constructor compacto: solo valores positivos. Los primeros
 * seis IDs de categoría están congelados por convenio (véase la cabecera de
 * {@code 02_data_seeding.sql}), de modo que las recetas existentes siguen
 * resolviendo su clave foránea {@code category_id} sin cambios al expandir los
 * datos maestros.
 */
public record CategoryId(int value) {
    public CategoryId {
        if (value <= 0) throw new IllegalArgumentException("CategoryId must be positive.");
    }
}
