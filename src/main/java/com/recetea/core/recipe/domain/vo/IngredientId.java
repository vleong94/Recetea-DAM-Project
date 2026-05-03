package com.recetea.core.recipe.domain.vo;

/**
 * Strongly-typed identifier for an {@code Ingredient} catalogue row. Wrapping the
 * database surrogate key in a record gives compile-time protection against the
 * cross-aggregate ID mix-ups that bare {@code int} parameters invite (a method
 * taking {@code IngredientId} cannot be called with a {@code RecipeId} or a
 * raw int).
 *
 * <p>Compact-constructor invariant: positive value only — {@code 0} and negatives
 * are programming mistakes given PostgreSQL's identity-column starts at {@code 1}.
 *
 * <p><b>ES — </b>Identificador fuertemente tipado para una fila del catálogo de
 * {@code Ingredient}. Envolver la clave subrogada en un record da protección en
 * tiempo de compilación frente a las mezclas de IDs entre agregados que invitan
 * los parámetros {@code int} desnudos (un método que recibe {@code IngredientId}
 * no puede invocarse con un {@code RecipeId} o un int crudo).
 *
 * <p>Invariante del constructor compacto: solo valores positivos — {@code 0} y
 * negativos son errores de programación dado que la columna de identidad de
 * PostgreSQL empieza en {@code 1}.
 */
public record IngredientId(int value) {
    public IngredientId {
        if (value <= 0) throw new IllegalArgumentException("IngredientId must be positive.");
    }
}
