package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.CategoryId;
import com.recetea.core.recipe.domain.vo.IngredientId;

/**
 * Catalogue entity — one row in {@code ingredients}. Carries an
 * {@code ingredient_category_id} for grouped picker UIs but is otherwise
 * the same shape as {@link Category} / {@link Difficulty}.
 *
 * <p>The seed ingredient set is curated and stable — IDs of the original
 * 85 rows are frozen for backwards compatibility with hard-coded recipe
 * references in test fixtures (see {@code CLAUDE.md}, "Database Setup").
 *
 * <p><b>ES — </b>Entidad de catálogo — una fila en {@code ingredients}.
 * Lleva un {@code ingredient_category_id} para los selectores agrupados en
 * la UI, pero por lo demás tiene la misma forma que {@link Category} /
 * {@link Difficulty}.
 *
 * <p>El conjunto de ingredientes semilla está curado y es estable — los IDs
 * de las 85 filas originales están congelados por compatibilidad con
 * referencias a recetas hard-codeadas en los fixtures de tests (véase
 * {@code CLAUDE.md}, sección "Database Setup").
 */
public record Ingredient(IngredientId id, CategoryId categoryId, String name) {

    public Ingredient {
        if (name == null || name.trim().isEmpty()) {
            throw new IngredientValidationException("Ingredient name is required.");
        }
        name = name.trim();
    }

    /** ComboBox display: show only the name, not the auto-generated record toString. */
    @Override
    public String toString() {
        return name;
    }

    public static class IngredientValidationException extends RuntimeException {
        public IngredientValidationException(String message) {
            super(message);
        }
    }
}
