package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.DifficultyId;

/**
 * Catalogue entity — one row in {@code recipe_difficulties} (Easy / Medium
 * / Hard / Expert). Same compact-constructor + {@code toString} pattern as
 * {@link Category}; the catalogue is enum-shaped (fixed row set) and
 * cached at the repository layer.
 *
 * <p><b>ES — </b>Entidad de catálogo — una fila en
 * {@code recipe_difficulties} (Fácil / Media / Difícil / Experto). Mismo
 * patrón de constructor compacto + {@code toString} que {@link Category};
 * el catálogo tiene forma de enum (conjunto fijo de filas) y se cachea en
 * la capa de repositorio.
 */
public record Difficulty(DifficultyId id, String name) {

    public Difficulty {
        if (name == null || name.trim().isEmpty()) {
            throw new DifficultyValidationException("Difficulty name is required.");
        }
        name = name.trim();
    }

    /** ComboBox display: show only the name, not the auto-generated record toString. */
    @Override
    public String toString() {
        return name;
    }

    public static class DifficultyValidationException extends RuntimeException {
        public DifficultyValidationException(String message) {
            super(message);
        }
    }
}
