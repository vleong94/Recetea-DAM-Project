package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.CategoryId;

/**
 * Catalogue entity — one row in {@code recipe_categories}. Compact
 * constructor enforces a non-blank name and trims whitespace so the field
 * is always render-ready.
 *
 * <p>{@link #toString()} returns the bare name so JavaFX {@code ComboBox}
 * cells render correctly without a custom converter — the auto-generated
 * record toString would expose internal field names.
 *
 * <p><b>ES — </b>Entidad de catálogo — una fila en {@code recipe_categories}.
 * El constructor compacto exige que el nombre no esté en blanco y aplica
 * trim al espacio en blanco, de modo que el campo siempre esté listo para
 * renderizar.
 *
 * <p>{@link #toString()} devuelve el nombre escueto para que las celdas de
 * los {@code ComboBox} de JavaFX se rendericen correctamente sin un
 * convertidor personalizado — el toString autogenerado del record expondría
 * los nombres internos de los campos.
 */
public record Category(CategoryId id, String name) {

    public Category {
        if (name == null || name.trim().isEmpty()) {
            throw new CategoryValidationException("Category name is required.");
        }
        name = name.trim();
    }

    /** ComboBox display: show only the name, not the auto-generated record toString. */
    @Override
    public String toString() {
        return name;
    }

    public static class CategoryValidationException extends RuntimeException {
        public CategoryValidationException(String message) {
            super(message);
        }
    }
}
