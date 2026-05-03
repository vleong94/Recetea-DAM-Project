package com.recetea.core.recipe.domain;

import com.recetea.core.recipe.domain.vo.IngredientId;
import com.recetea.core.recipe.domain.vo.UnitId;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Junction-table row connecting a recipe to one ingredient + unit + quantity.
 *
 * <p>The two display fields ({@code ingredientName}, {@code unitAbbreviation})
 * are denormalised onto the row at hydration time so the detail / PDF
 * paths render without a second catalogue lookup. They are nullable
 * because the 3-arg domain-creation constructor doesn't know them — only
 * the LATERAL JSONB hydration path populates them.
 *
 * <p>Compact-constructor invariants: quantity strictly &gt; 0; if the
 * display fields are present they must be non-blank (a present-but-blank
 * value indicates a JOIN bug, not a legitimate state).
 *
 * <p><b>ES — </b>Fila de la tabla de unión que conecta una receta con un
 * ingrediente + unidad + cantidad.
 *
 * <p>Los dos campos de presentación ({@code ingredientName},
 * {@code unitAbbreviation}) se desnormalizan sobre la fila en el momento de
 * la hidratación para que las rutas de detalle / PDF rendericen sin una
 * segunda consulta al catálogo. Son anulables porque el constructor de
 * creación de dominio de 3 args no los conoce — sólo la ruta de hidratación
 * LATERAL JSONB los rellena.
 *
 * <p>Invariantes del constructor compacto: cantidad estrictamente &gt; 0;
 * si los campos de presentación están presentes deben no estar en blanco
 * (un valor presente pero en blanco indica un bug de JOIN, no un estado
 * legítimo).
 */
public record RecipeIngredient(
        IngredientId ingredientId,
        UnitId unitId,
        BigDecimal quantity,
        String ingredientName,
        String unitAbbreviation
) {
    public RecipeIngredient {
        Objects.requireNonNull(ingredientId, "ingredientId is required.");
        Objects.requireNonNull(unitId,       "unitId is required.");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RecipeIngredientValidationException(
                    "Ingredient quantity must be strictly greater than zero.");
        }
        if (ingredientName != null && ingredientName.isBlank()) {
            throw new RecipeIngredientValidationException(
                    "Ingredient name is required for deep-load instantiation.");
        }
        if (unitAbbreviation != null && unitAbbreviation.isBlank()) {
            throw new RecipeIngredientValidationException(
                    "Unit abbreviation is required for deep-load instantiation.");
        }
        ingredientName    = ingredientName    != null ? ingredientName.trim()    : null;
        unitAbbreviation  = unitAbbreviation  != null ? unitAbbreviation.trim()  : null;
    }

    public RecipeIngredient(IngredientId ingredientId, UnitId unitId, BigDecimal quantity) {
        this(ingredientId, unitId, quantity, null, null);
    }

    public static class RecipeIngredientValidationException extends InvalidIngredientException {
        public RecipeIngredientValidationException(String message) {
            super(message);
        }
    }
}
