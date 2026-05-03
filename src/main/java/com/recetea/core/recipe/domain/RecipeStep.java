package com.recetea.core.recipe.domain;

/**
 * Single step in a recipe. {@code stepOrder} is 1-based and unique within
 * a recipe — duplicate ordinals are rejected by {@code Recipe.syncSteps()}
 * during persistence. The compact constructor trims whitespace so the
 * field is render-ready and rejects positive non-integer ordinals upstream
 * of the SQL layer.
 *
 * <p><b>ES — </b>Un paso individual de una receta. {@code stepOrder} es
 * 1-based y único dentro de una receta — los ordinales duplicados los
 * rechaza {@code Recipe.syncSteps()} durante la persistencia. El
 * constructor compacto aplica trim al espacio en blanco para que el campo
 * esté listo para renderizar y rechaza ordinales no positivos antes de
 * llegar a la capa SQL.
 */
public record RecipeStep(int stepOrder, String instruction) {

    public RecipeStep {
        if (stepOrder <= 0) {
            throw new InvalidRecipeStepException("Step order must be a positive value.");
        }

        if (instruction == null || instruction.trim().isEmpty()) {
            throw new InvalidRecipeStepException("Step instruction must not be null or empty.");
        }

        // Normalise whitespace before storing.
        instruction = instruction.trim();
    }

    public static class InvalidRecipeStepException extends RuntimeException {
        public InvalidRecipeStepException(String message) {
            super(message);
        }
    }
}
